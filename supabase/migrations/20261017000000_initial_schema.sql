-- House of Vino — initial server schema.
--
-- Security model (docs/ADR.md, ADR-06/07):
--   * Every table has row-level security. A user sees rows of collections they are a member of;
--     owners and editors may write; viewers may only read; nobody may hard-delete synced rows.
--   * The movement ledger is append-only (no UPDATE/DELETE policies + a trigger).
--   * `rev` is assigned by trigger from one sequence, under a per-collection advisory lock, so
--     revisions within a collection become visible in order (pull cursors never skip rows).
--   * Child rows reference parents through composite (collection_id, id) keys, so a row can never
--     point into another collection.
--   * The app holds only the anon key; privileged work runs in SECURITY DEFINER functions with
--     explicit checks, or in the delete-account Edge Function with the service role.

create sequence if not exists public.sync_rev_seq;

-- ---------------------------------------------------------------------------------------------
-- Collections and households

create table public.collection (
    id uuid primary key,
    name text not null check (length(btrim(name)) between 1 and 80),
    owner_id uuid not null references auth.users (id) on delete cascade,
    created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
    updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
    deleted_at bigint,
    rev bigint not null default 0
);

create table public.collection_member (
    collection_id uuid not null references public.collection (id) on delete cascade,
    user_id uuid not null references auth.users (id) on delete cascade,
    role text not null check (role in ('owner', 'editor', 'viewer')),
    added_at timestamptz not null default now(),
    primary key (collection_id, user_id)
);
create index collection_member_user on public.collection_member (user_id);

create table public.collection_invite (
    id uuid primary key default gen_random_uuid(),
    collection_id uuid not null references public.collection (id) on delete cascade,
    email text not null check (email = lower(btrim(email))),
    role text not null check (role in ('editor', 'viewer')),
    token_hash text not null unique,
    expires_at timestamptz not null,
    accepted_at timestamptz,
    created_by uuid not null references auth.users (id) on delete cascade,
    created_at timestamptz not null default now()
);

-- Membership helpers. SECURITY DEFINER so policies can consult collection_member without
-- recursive RLS; search_path is pinned to prevent hijacking.
create function public.is_member(cid uuid) returns boolean
language sql stable security definer set search_path = '' as $$
    select exists (select 1 from public.collection_member m where m.collection_id = cid and m.user_id = auth.uid());
$$;

create function public.can_write(cid uuid) returns boolean
language sql stable security definer set search_path = '' as $$
    select exists (select 1 from public.collection_member m
                   where m.collection_id = cid and m.user_id = auth.uid() and m.role in ('owner', 'editor'));
$$;

create function public.is_owner(cid uuid) returns boolean
language sql stable security definer set search_path = '' as $$
    select exists (select 1 from public.collection_member m
                   where m.collection_id = cid and m.user_id = auth.uid() and m.role = 'owner');
$$;

create function public.try_uuid(t text) returns uuid
language plpgsql immutable set search_path = '' as $$
begin
    return t::uuid;
exception when others then
    return null;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Revision assignment and ledger immutability

create function public.assign_rev() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    -- Serialise writers per collection so revs commit in order within a collection.
    perform pg_advisory_xact_lock(hashtextextended(new.collection_id::text, 0));
    new.rev := nextval('public.sync_rev_seq');
    new.server_updated_at := now();
    return new;
end;
$$;

create function public.movement_immutable() returns trigger
language plpgsql set search_path = '' as $$
begin
    -- Allow only cascades from deleting a whole collection (account deletion).
    if tg_op = 'DELETE' and pg_trigger_depth() > 1 then
        return old;
    end if;
    raise exception 'movement rows are append-only' using errcode = '42501';
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Synced tables. Columns mirror the client schema (data/src/commonMain/sqldelight/com/houseofvino/data/db/1.sqm).

create table public.wine (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    producer text not null check (length(btrim(producer)) > 0),
    producer_sort text not null,
    name text,
    color text check (color in ('RED', 'WHITE', 'ROSE', 'ORANGE')),
    style text check (style in ('STILL', 'SPARKLING', 'FORTIFIED', 'SWEET')),
    country text, region text, sub_region text, appellation text, classification text, notes text,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id)
);

create table public.wine_grape (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    wine_id uuid not null,
    grape text not null check (length(btrim(grape)) > 0),
    percent integer check (percent is null or percent between 0 and 100),
    position integer not null default 0,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    foreign key (collection_id, wine_id) references public.wine (collection_id, id) deferrable initially deferred
);

create table public.vintage (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    wine_id uuid not null,
    vintage_kind text not null check (vintage_kind in ('YEAR', 'NV', 'UNKNOWN')),
    year integer check ((vintage_kind = 'YEAR') = (year is not null)),
    drink_from integer, drink_until integer, drink_window_source text,
    abv_bp integer check (abv_bp is null or abv_bp between 0 and 8000),
    notes text, label_photo_id uuid,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    check (drink_from is null or drink_until is null or drink_from <= drink_until),
    foreign key (collection_id, wine_id) references public.wine (collection_id, id) deferrable initially deferred
);

create table public.barcode (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    code text not null, symbology text not null,
    wine_id uuid not null, vintage_id uuid, format_ml integer,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    foreign key (collection_id, wine_id) references public.wine (collection_id, id) deferrable initially deferred,
    foreign key (collection_id, vintage_id) references public.vintage (collection_id, id) deferrable initially deferred
);

create table public.tag (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    name text not null check (length(btrim(name)) between 1 and 40),
    color text,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id)
);

create table public.vintage_tag (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    vintage_id uuid not null, tag_id uuid not null,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    foreign key (collection_id, vintage_id) references public.vintage (collection_id, id) deferrable initially deferred,
    foreign key (collection_id, tag_id) references public.tag (collection_id, id) deferrable initially deferred
);

create table public.location (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    parent_id uuid,
    name text not null check (length(btrim(name)) > 0),
    kind text not null check (kind in ('CELLAR', 'FRIDGE', 'RACK', 'SHELF', 'BIN', 'CASE', 'BOX', 'OTHER')),
    grid_rows integer check (grid_rows is null or grid_rows between 1 and 200),
    grid_cols integer check (grid_cols is null or grid_cols between 1 and 200),
    sort_order integer not null default 0, notes text,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    check ((grid_rows is null) = (grid_cols is null)),
    foreign key (collection_id, parent_id) references public.location (collection_id, id) deferrable initially deferred
);

create table public.wishlist_item (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    vintage_id uuid, wine_id uuid, producer text, name text,
    vintage_kind text check (vintage_kind is null or vintage_kind in ('YEAR', 'NV', 'UNKNOWN')),
    year integer, format_ml integer,
    desired_qty integer not null default 1 check (desired_qty >= 1),
    target_price_minor bigint check (target_price_minor is null or target_price_minor >= 0),
    target_price_currency text check (target_price_currency is null or target_price_currency ~ '^[A-Z]{3}$'),
    priority text not null check (priority in ('LOW', 'MEDIUM', 'HIGH', 'MUST')),
    status text not null check (status in ('WANTED', 'WATCHING', 'ORDERED', 'ACQUIRED', 'CANCELLED')),
    notes text, acquired_lot_id uuid,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    check (vintage_id is not null or wine_id is not null or length(btrim(coalesce(producer, ''))) > 0),
    check ((target_price_minor is null) = (target_price_currency is null)),
    foreign key (collection_id, vintage_id) references public.vintage (collection_id, id) deferrable initially deferred,
    foreign key (collection_id, wine_id) references public.wine (collection_id, id) deferrable initially deferred
);

create table public.merchant_link (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    wishlist_item_id uuid not null,
    merchant text not null, url text check (url is null or url ~* '^https?://'),
    last_price_minor bigint, last_price_currency text, checked_on date,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    check ((last_price_minor is null) = (last_price_currency is null)),
    foreign key (collection_id, wishlist_item_id) references public.wishlist_item (collection_id, id) deferrable initially deferred
);

create table public.lot (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    vintage_id uuid not null,
    format_ml integer not null check (format_ml > 0),
    acquired_on date, source text, order_ref text,
    unit_price_minor bigint check (unit_price_minor is null or unit_price_minor >= 0),
    unit_price_currency text check (unit_price_currency is null or unit_price_currency ~ '^[A-Z]{3}$'),
    packaging text check (packaging in ('LOOSE', 'OWC', 'OC', 'CARTON')),
    bottles_per_case integer check (bottles_per_case is null or bottles_per_case > 0),
    wishlist_item_id uuid, notes text,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    check ((unit_price_minor is null) = (unit_price_currency is null)),
    foreign key (collection_id, vintage_id) references public.vintage (collection_id, id) deferrable initially deferred
);

create table public.movement (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    lot_id uuid not null,
    location_id uuid,
    delta integer not null check (delta <> 0),
    kind text not null check (kind in ('PURCHASE', 'OPENING', 'CONSUME', 'SELL', 'GIFT_OUT', 'GIFT_IN', 'LOSS',
                                       'TRANSFER_OUT', 'TRANSFER_IN', 'ADJUST', 'REVERSAL')),
    occurred_at bigint not null,
    transfer_group_id uuid,
    reversal_of uuid,
    amount_minor bigint, amount_currency text,
    counterparty text, note text,
    device_id uuid not null,
    tasting_id uuid,
    created_at bigint not null,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    check (kind not in ('PURCHASE', 'OPENING', 'GIFT_IN', 'TRANSFER_IN') or delta > 0),
    check (kind not in ('CONSUME', 'SELL', 'GIFT_OUT', 'LOSS', 'TRANSFER_OUT') or delta < 0),
    check ((kind in ('TRANSFER_IN', 'TRANSFER_OUT')) = (transfer_group_id is not null)),
    check ((kind = 'REVERSAL') = (reversal_of is not null)),
    check ((amount_minor is null) = (amount_currency is null)),
    foreign key (collection_id, lot_id) references public.lot (collection_id, id) deferrable initially deferred,
    foreign key (collection_id, location_id) references public.location (collection_id, id) deferrable initially deferred,
    foreign key (collection_id, reversal_of) references public.movement (collection_id, id) deferrable initially deferred
);

create table public.slot (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    location_id uuid not null,
    slot_row integer not null check (slot_row >= 1),
    slot_col integer not null check (slot_col >= 1),
    lot_id uuid,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    foreign key (collection_id, location_id) references public.location (collection_id, id) deferrable initially deferred,
    foreign key (collection_id, lot_id) references public.lot (collection_id, id) deferrable initially deferred
);

create table public.tasting (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    vintage_id uuid not null,
    tasted_on date not null,
    rating integer check (rating is null or rating between 0 and 100),
    appearance text, nose_intensity text, sweetness text, acidity text, tannin text, alcohol text, body text,
    flavour_intensity text, finish text, aromas text, notes text, occasion text, food_pairing text,
    companions text, place text, format_ml integer, movement_id uuid, would_buy_again integer,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    foreign key (collection_id, vintage_id) references public.vintage (collection_id, id) deferrable initially deferred
);

create table public.valuation (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    vintage_id uuid not null,
    format_ml integer not null,
    amount_minor bigint not null check (amount_minor >= 0),
    amount_currency text not null check (amount_currency ~ '^[A-Z]{3}$'),
    source text not null check (length(btrim(source)) > 0),
    as_of date not null,
    note text,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    foreign key (collection_id, vintage_id) references public.vintage (collection_id, id) deferrable initially deferred
);

create table public.photo (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    owner_type text not null check (owner_type in ('VINTAGE', 'TASTING', 'LOT')),
    owner_id uuid not null,
    remote_path text check (remote_path is null or split_part(remote_path, '/', 1) = collection_id::text),
    mime text not null, width integer not null, height integer not null, bytes bigint not null,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id)
);

create table public.exchange_rate (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    base text not null check (base ~ '^[A-Z]{3}$'),
    quote text not null check (quote ~ '^[A-Z]{3}$'),
    rate_e8 bigint not null check (rate_e8 > 0),
    as_of date not null,
    source text not null check (length(btrim(source)) > 0),
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id),
    check (base <> quote)
);

create table public.saved_search (
    id uuid primary key,
    collection_id uuid not null references public.collection (id) on delete cascade,
    name text not null, scope text not null, query_text text not null, filters_json text not null, sort text not null,
    created_at bigint not null, updated_at bigint not null, deleted_at bigint,
    rev bigint not null, server_updated_at timestamptz not null,
    unique (collection_id, id)
);

-- Idempotency ledger for sync_push: one row per applied op, per user.
create table public.sync_applied_op (
    user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    op_id uuid not null,
    collection_id uuid not null references public.collection (id) on delete cascade,
    result jsonb not null,
    applied_at timestamptz not null default now(),
    primary key (user_id, op_id)
);

-- ---------------------------------------------------------------------------------------------
-- Triggers, indexes and policies for all synced tables

do $$
declare
    t text;
    synced text[] := array['wine', 'wine_grape', 'vintage', 'barcode', 'tag', 'vintage_tag', 'location',
                           'wishlist_item', 'merchant_link', 'lot', 'movement', 'slot', 'tasting', 'valuation',
                           'photo', 'exchange_rate', 'saved_search'];
begin
    foreach t in array synced loop
        execute format('create trigger %I before insert or update on public.%I for each row execute function public.assign_rev()',
                       t || '_assign_rev', t);
        execute format('create index %I on public.%I (collection_id, rev)', t || '_pull', t);
        execute format('alter table public.%I enable row level security', t);
        execute format('create policy %I on public.%I for select to authenticated using (public.is_member(collection_id))',
                       t || '_read', t);
        execute format('create policy %I on public.%I for insert to authenticated with check (public.can_write(collection_id))',
                       t || '_insert', t);
        if t <> 'movement' then
            execute format('create policy %I on public.%I for update to authenticated using (public.can_write(collection_id)) with check (public.can_write(collection_id))',
                           t || '_update', t);
        end if;
        -- No delete policies: rows are tombstoned with deleted_at and removed only with their collection.
        -- Explicit privileges (Supabase's defaults grant everything; RLS alone would make a
        -- DELETE silently match nothing, so refuse it outright instead).
        execute format('revoke all on public.%I from anon, authenticated', t);
        if t = 'movement' then
            execute format('grant select, insert on public.%I to authenticated', t);
        else
            execute format('grant select, insert, update on public.%I to authenticated', t);
        end if;
    end loop;
end;
$$;

create trigger movement_immutable before update or delete on public.movement
for each row execute function public.movement_immutable();

alter table public.collection enable row level security;
create policy collection_read on public.collection for select to authenticated using (public.is_member(id));
create policy collection_rename on public.collection for update to authenticated using (public.is_owner(id)) with check (public.is_owner(id));
revoke all on public.collection from anon, authenticated;
grant select, update (name, updated_at) on public.collection to authenticated;

alter table public.collection_member enable row level security;
create policy member_read on public.collection_member for select to authenticated using (public.is_member(collection_id));
revoke all on public.collection_member from anon, authenticated;
grant select on public.collection_member to authenticated;

alter table public.collection_invite enable row level security;
create policy invite_read on public.collection_invite for select to authenticated using (public.is_owner(collection_id));
revoke all on public.collection_invite from anon, authenticated;
grant select on public.collection_invite to authenticated;

alter table public.sync_applied_op enable row level security;
create policy applied_own_read on public.sync_applied_op for select to authenticated using (user_id = auth.uid());
create policy applied_own_insert on public.sync_applied_op for insert to authenticated
    with check (user_id = auth.uid() and public.is_member(collection_id));
revoke all on public.sync_applied_op from anon, authenticated;
grant select, insert on public.sync_applied_op to authenticated;

revoke usage on sequence public.sync_rev_seq from anon, authenticated;
