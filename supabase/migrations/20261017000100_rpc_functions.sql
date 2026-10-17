-- RPC functions: collection bootstrap, households, sync push, account data deletion, storage policies.

-- ---------------------------------------------------------------------------------------------
-- Collections

-- Registers this user's personal collection with the id the app already uses offline.
-- Idempotent. Refuses ids that belong to someone else.
create function public.bootstrap_collection(p_collection_id uuid, p_name text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'Sign in required' using errcode = '42501';
    end if;
    if exists (select 1 from public.collection where id = p_collection_id) then
        if not exists (select 1 from public.collection_member where collection_id = p_collection_id and user_id = uid) then
            raise exception 'That collection id is already in use' using errcode = '42501';
        end if;
        return;
    end if;
    insert into public.collection (id, name, owner_id) values (p_collection_id, coalesce(nullif(btrim(p_name), ''), 'My cellar'), uid);
    insert into public.collection_member (collection_id, user_id, role) values (p_collection_id, uid, 'owner');
end;
$$;

create function public.my_collections() returns jsonb
language sql stable security definer set search_path = '' as $$
    select coalesce(jsonb_agg(jsonb_build_object(
               'collection_id', c.id, 'name', c.name, 'role', m.role, 'owner_id', c.owner_id) order by c.created_at), '[]'::jsonb)
    from public.collection_member m join public.collection c on c.id = m.collection_id
    where m.user_id = auth.uid() and c.deleted_at is null;
$$;

-- ---------------------------------------------------------------------------------------------
-- Households: invites and membership

-- Returns a one-time token (shown once). Only its SHA-256 hash is stored.
create function public.create_invite(p_collection_id uuid, p_email text, p_role text) returns text
language plpgsql security definer set search_path = '' as $$
declare
    token text := replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', '');
begin
    if not public.is_owner(p_collection_id) then
        raise exception 'Only the owner can invite people' using errcode = '42501';
    end if;
    if p_role not in ('editor', 'viewer') then
        raise exception 'Role must be editor or viewer' using errcode = '22023';
    end if;
    insert into public.collection_invite (collection_id, email, role, token_hash, expires_at, created_by)
    values (p_collection_id, lower(btrim(p_email)), p_role, encode(sha256(convert_to(token, 'UTF8')), 'hex'),
            now() + interval '7 days', auth.uid());
    return token;
end;
$$;

-- Accepts an invite addressed to the signed-in user's email. Returns the collection id.
create function public.accept_invite(p_token text) returns uuid
language plpgsql security definer set search_path = '' as $$
declare
    inv public.collection_invite%rowtype;
    my_email text := lower(coalesce(auth.jwt() ->> 'email', ''));
begin
    if auth.uid() is null then
        raise exception 'Sign in required' using errcode = '42501';
    end if;
    select * into inv from public.collection_invite
    where token_hash = encode(sha256(convert_to(btrim(p_token), 'UTF8')), 'hex')
    for update;
    if not found or inv.accepted_at is not null or inv.expires_at < now() then
        raise exception 'This invite is invalid or has expired' using errcode = '22023';
    end if;
    if inv.email <> my_email then
        raise exception 'This invite was sent to a different email address' using errcode = '42501';
    end if;
    insert into public.collection_member (collection_id, user_id, role)
    values (inv.collection_id, auth.uid(), inv.role)
    on conflict (collection_id, user_id) do nothing; -- never downgrade an existing owner/editor
    update public.collection_invite set accepted_at = now() where id = inv.id;
    return inv.collection_id;
end;
$$;

-- Owners can remove members; any member can remove themselves (leave). Owners cannot leave.
create function public.remove_member(p_collection_id uuid, p_user_id uuid) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not (public.is_owner(p_collection_id) or p_user_id = auth.uid()) then
        raise exception 'Only the owner can remove members' using errcode = '42501';
    end if;
    if exists (select 1 from public.collection where id = p_collection_id and owner_id = p_user_id) then
        raise exception 'The owner cannot be removed' using errcode = '22023';
    end if;
    delete from public.collection_member where collection_id = p_collection_id and user_id = p_user_id;
end;
$$;

create function public.set_member_role(p_collection_id uuid, p_user_id uuid, p_role text) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_owner(p_collection_id) then
        raise exception 'Only the owner can change roles' using errcode = '42501';
    end if;
    if p_role not in ('editor', 'viewer') then
        raise exception 'Role must be editor or viewer' using errcode = '22023';
    end if;
    update public.collection_member set role = p_role
    where collection_id = p_collection_id and user_id = p_user_id and role <> 'owner';
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Sync push
--
-- SECURITY INVOKER: every write is checked by the same RLS policies as direct table access.
-- Input: p_ops = [{op_id, entity, id, base_rev, row}], applied in order, each in its own
-- subtransaction. Output: [{op_id, status: applied|conflict|rejected, rev?, server_row?, code?, message?, permanent?}]

create function public.sync_push(p_collection_id uuid, p_ops jsonb) returns jsonb
language plpgsql security invoker set search_path = '' as $$
declare
    allowed constant text[] := array['wine', 'wine_grape', 'vintage', 'barcode', 'tag', 'vintage_tag', 'location',
        'wishlist_item', 'merchant_link', 'lot', 'movement', 'slot', 'tasting', 'valuation', 'photo',
        'exchange_rate', 'saved_search'];
    op jsonb;
    results jsonb := '[]'::jsonb;
    res jsonb;
    prior jsonb;
    ent text;
    rid uuid;
    op_uuid uuid;
    base bigint;
    cur_rev bigint;
    new_rev bigint;
    server_row jsonb;
    cols text;
    rcols text;
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'Sign in required' using errcode = '42501';
    end if;
    if jsonb_typeof(p_ops) <> 'array' or jsonb_array_length(p_ops) > 500 then
        raise exception 'p_ops must be an array of at most 500 operations' using errcode = '22023';
    end if;
    -- Check foreign keys per statement so a violation is caught by that op's handler below.
    set constraints all immediate;

    for op in select value from jsonb_array_elements(p_ops) loop
        op_uuid := public.try_uuid(op ->> 'op_id');
        ent := op ->> 'entity';
        rid := public.try_uuid(op ->> 'id');
        base := nullif(op ->> 'base_rev', '')::bigint;
        res := null;

        if op_uuid is null or rid is null then
            res := jsonb_build_object('status', 'rejected', 'code', '22023', 'message', 'Malformed operation', 'permanent', true);
        else
            select a.result into prior from public.sync_applied_op a where a.user_id = uid and a.op_id = op_uuid;
            if found then
                results := results || jsonb_build_array(prior);
                continue;
            end if;
        end if;

        if res is null and not (ent = any (allowed)) then
            res := jsonb_build_object('status', 'rejected', 'code', '22023', 'message', 'Unknown entity ' || coalesce(ent, 'null'), 'permanent', true);
        end if;
        if res is null and (public.try_uuid(op -> 'row' ->> 'collection_id') is distinct from p_collection_id
                            or public.try_uuid(op -> 'row' ->> 'id') is distinct from rid) then
            res := jsonb_build_object('status', 'rejected', 'code', '22023', 'message', 'Row does not match the operation', 'permanent', true);
        end if;
        if res is null and not public.can_write(p_collection_id) then
            res := jsonb_build_object('status', 'rejected', 'code', '42501', 'message', 'You can view this collection but not change it', 'permanent', true);
        end if;

        if res is null then
            begin
                if ent = 'movement' then
                    execute 'insert into public.movement select * from jsonb_populate_record(null::public.movement, $1) '
                         || 'on conflict (id) do nothing returning rev'
                        using op -> 'row' into new_rev;
                    if new_rev is null then
                        select m.rev into new_rev from public.movement m where m.id = rid and m.collection_id = p_collection_id;
                        if new_rev is null then
                            raise exception 'Movement id already used' using errcode = '42501';
                        end if;
                    end if;
                    res := jsonb_build_object('status', 'applied', 'rev', new_rev);
                else
                    execute format('select t.rev from public.%I t where t.id = $1 for update', ent) using rid into cur_rev;
                    if cur_rev is not null and cur_rev is distinct from base then
                        execute format('select to_jsonb(t) from public.%I t where t.id = $1', ent) using rid into server_row;
                        res := jsonb_build_object('status', 'conflict', 'server_row', server_row);
                    elsif cur_rev is null then
                        execute format('insert into public.%I select * from jsonb_populate_record(null::public.%I, $1) returning rev', ent, ent)
                            using op -> 'row' into new_rev;
                        res := jsonb_build_object('status', 'applied', 'rev', new_rev);
                    else
                        select string_agg(quote_ident(a.attname), ', ' order by a.attnum),
                               string_agg('r.' || quote_ident(a.attname), ', ' order by a.attnum)
                          into cols, rcols
                          from pg_attribute a
                         where a.attrelid = ('public.' || quote_ident(ent))::regclass and a.attnum > 0 and not a.attisdropped
                           and a.attname not in ('id', 'collection_id', 'rev', 'server_updated_at', 'created_at');
                        execute format('update public.%I t set (%s) = (select %s from jsonb_populate_record(null::public.%I, $1) r) '
                                    || 'where t.id = $2 returning t.rev', ent, cols, rcols, ent)
                            using op -> 'row', rid into new_rev;
                        res := jsonb_build_object('status', 'applied', 'rev', new_rev);
                    end if;
                end if;
            exception
                when foreign_key_violation then
                    -- Usually the parent row hasn't arrived yet: retry later, don't record the op.
                    res := jsonb_build_object('status', 'rejected', 'code', sqlstate, 'message', sqlerrm, 'permanent', false);
                when insufficient_privilege or check_violation or not_null_violation
                     or unique_violation or invalid_text_representation or datetime_field_overflow
                     or invalid_datetime_format or numeric_value_out_of_range or raise_exception then
                    res := jsonb_build_object('status', 'rejected', 'code', sqlstate, 'message', sqlerrm, 'permanent', true);
            end;
        end if;

        res := res || jsonb_build_object('op_id', op ->> 'op_id');
        -- Remember outcomes that must not be re-evaluated on retry. Rejections are deterministic
        -- and are simply re-checked if the client sends the op again.
        if op_uuid is not null and res ->> 'status' in ('applied', 'conflict') then
            insert into public.sync_applied_op (user_id, op_id, collection_id, result)
            values (uid, op_uuid, p_collection_id, res)
            on conflict do nothing;
        end if;
        results := results || jsonb_build_array(res);
    end loop;
    return results;
end;
$$;

comment on function public.sync_push(uuid, jsonb) is
    'Applies queued client changes with idempotent op ids and optimistic concurrency. RLS applies.';

-- ---------------------------------------------------------------------------------------------
-- Account deletion (called only by the delete-account Edge Function with the service role)

create function public.delete_user_data(p_user_id uuid) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    owned uuid[];
begin
    select coalesce(array_agg(id), '{}') into owned from public.collection where owner_id = p_user_id;
    delete from public.collection where id = any (owned);           -- cascades every synced row
    delete from public.collection_member where user_id = p_user_id;  -- leave shared households
    delete from public.sync_applied_op where user_id = p_user_id;
    return jsonb_build_object('deleted_collections', to_jsonb(owned));
end;
$$;
revoke all on function public.delete_user_data(uuid) from public, anon, authenticated;

-- Function privileges: RPCs for signed-in users only.
revoke all on function public.bootstrap_collection(uuid, text), public.my_collections(), public.create_invite(uuid, text, text),
    public.accept_invite(text), public.remove_member(uuid, uuid), public.set_member_role(uuid, uuid, text),
    public.sync_push(uuid, jsonb) from public, anon;
grant execute on function public.bootstrap_collection(uuid, text), public.my_collections(), public.create_invite(uuid, text, text),
    public.accept_invite(text), public.remove_member(uuid, uuid), public.set_member_role(uuid, uuid, text),
    public.sync_push(uuid, jsonb) to authenticated;

-- ---------------------------------------------------------------------------------------------
-- Photo storage: private bucket, objects at {collection_id}/{photo_id}.jpg

insert into storage.buckets (id, name, public) values ('photos', 'photos', false) on conflict (id) do nothing;

create policy photos_read on storage.objects for select to authenticated
    using (bucket_id = 'photos' and public.is_member(public.try_uuid((storage.foldername(name))[1])));
create policy photos_insert on storage.objects for insert to authenticated
    with check (bucket_id = 'photos' and public.can_write(public.try_uuid((storage.foldername(name))[1])));
create policy photos_update on storage.objects for update to authenticated
    using (bucket_id = 'photos' and public.can_write(public.try_uuid((storage.foldername(name))[1])));
