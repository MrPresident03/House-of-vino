-- Server contract tests: RLS, households, sync_push semantics, ledger immutability, deletion.
-- Run with supabase/tests/run.sh against a scratch database. Any failed assertion aborts.
\set ON_ERROR_STOP 1
\set QUIET 1

reset role;
create schema if not exists test;
grant usage on schema test to authenticated, anon;

create or replace function test.become(uid uuid, email text) returns void language plpgsql as $$
begin
    perform set_config('request.jwt.claims', json_build_object('sub', uid, 'email', email, 'role', 'authenticated')::text, false);
end $$;

create or replace function test.eq(actual anyelement, expected anyelement, what text) returns void language plpgsql as $$
begin
    if actual is distinct from expected then
        raise exception 'FAILED: % — expected %, got %', what, expected, actual;
    end if;
    raise notice 'ok: %', what;
end $$;

create or replace function test.fails(stmt text, what text) returns void language plpgsql as $$
begin
    begin
        execute stmt;
    exception when others then
        raise notice 'ok: % (%: %)', what, sqlstate, sqlerrm;
        return;
    end;
    raise exception 'FAILED: % — statement succeeded: %', what, stmt;
end $$;
grant execute on all functions in schema test to authenticated, anon;

insert into auth.users (id, email) values
    ('a0000000-0000-4000-8000-000000000001', 'alice@example.com'),
    ('b0000000-0000-4000-8000-000000000002', 'bob@example.com'),
    ('c0000000-0000-4000-8000-000000000003', 'carol@example.com'),
    ('e0000000-0000-4000-8000-000000000004', 'eve@example.com');

-- Row builders (as the client sends them).
create or replace function test.wine_row(cid uuid, wid uuid, producer text) returns jsonb language sql as $$
    select jsonb_build_object('id', wid, 'collection_id', cid, 'producer', producer, 'producer_sort', lower(producer),
        'name', null, 'color', 'RED', 'style', null, 'country', 'France', 'region', null, 'sub_region', null,
        'appellation', null, 'classification', null, 'notes', null, 'created_at', 1, 'updated_at', 1, 'deleted_at', null, 'rev', null)
$$;
create or replace function test.vintage_row(cid uuid, vid uuid, wid uuid, yr int) returns jsonb language sql as $$
    select jsonb_build_object('id', vid, 'collection_id', cid, 'wine_id', wid, 'vintage_kind', 'YEAR', 'year', yr,
        'drink_from', null, 'drink_until', null, 'drink_window_source', null, 'abv_bp', null, 'notes', null,
        'label_photo_id', null, 'created_at', 1, 'updated_at', 1, 'deleted_at', null, 'rev', null)
$$;
create or replace function test.lot_row(cid uuid, lid uuid, vid uuid) returns jsonb language sql as $$
    select jsonb_build_object('id', lid, 'collection_id', cid, 'vintage_id', vid, 'format_ml', 750, 'acquired_on', '2018-05-01',
        'source', 'Merchant', 'order_ref', null, 'unit_price_minor', 45000, 'unit_price_currency', 'EUR', 'packaging', null,
        'bottles_per_case', null, 'wishlist_item_id', null, 'notes', null, 'created_at', 1, 'updated_at', 1, 'deleted_at', null, 'rev', null)
$$;
create or replace function test.movement_row(cid uuid, mid uuid, lid uuid, delta int, kind text) returns jsonb language sql as $$
    select jsonb_build_object('id', mid, 'collection_id', cid, 'lot_id', lid, 'location_id', null, 'delta', delta, 'kind', kind,
        'occurred_at', 1, 'transfer_group_id', null, 'reversal_of', null, 'amount_minor', null, 'amount_currency', null,
        'counterparty', null, 'note', null, 'device_id', 'd0000000-0000-4000-8000-0000000000d1', 'tasting_id', null,
        'created_at', 1, 'rev', null)
$$;
create or replace function test.op(opid uuid, entity text, id uuid, base bigint, r jsonb) returns jsonb language sql as $$
    select jsonb_build_object('op_id', opid, 'entity', entity, 'id', id, 'base_rev', base, 'row', r)
$$;
grant execute on all functions in schema test to authenticated, anon;

-- ============================================================================================
-- Alice bootstraps her collection and pushes a wine with stock.
set role authenticated;
select test.become('a0000000-0000-4000-8000-000000000001', 'alice@example.com');
select public.bootstrap_collection('aaaaaaaa-0000-4000-8000-000000000001', 'Alice''s cellar');
select public.bootstrap_collection('aaaaaaaa-0000-4000-8000-000000000001', 'Alice''s cellar'); -- idempotent
select test.eq((select count(*) from public.collection)::int, 1, 'alice sees her collection');
select test.eq((public.my_collections() -> 0 ->> 'role'), 'owner', 'alice is owner');

create temp table r1 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000f1', 'wine', '11111111-0000-4000-8000-000000000001', null,
        test.wine_row('aaaaaaaa-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 'Château Margaux')),
    test.op('00000000-0000-4000-8000-0000000000f2', 'vintage', '22222222-0000-4000-8000-000000000001', null,
        test.vintage_row('aaaaaaaa-0000-4000-8000-000000000001', '22222222-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 2015)),
    test.op('00000000-0000-4000-8000-0000000000f3', 'lot', '33333333-0000-4000-8000-000000000001', null,
        test.lot_row('aaaaaaaa-0000-4000-8000-000000000001', '33333333-0000-4000-8000-000000000001', '22222222-0000-4000-8000-000000000001')),
    test.op('00000000-0000-4000-8000-0000000000f4', 'movement', '44444444-0000-4000-8000-000000000001', null,
        test.movement_row('aaaaaaaa-0000-4000-8000-000000000001', '44444444-0000-4000-8000-000000000001', '33333333-0000-4000-8000-000000000001', 6, 'PURCHASE'))
)) as res;
select test.eq((select count(*) from r1, jsonb_array_elements(res) e where e ->> 'status' = 'applied')::int, 4, 'four ops applied');
select test.eq((select (res -> 1 ->> 'rev')::bigint > (res -> 0 ->> 'rev')::bigint from r1), true, 'revs increase in order');
select test.eq((select acquired_on from public.lot)::text, '2018-05-01', 'dates round-trip');

-- Idempotent replay: same op ids return the same results and change nothing.
create temp table r2 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000f1', 'wine', '11111111-0000-4000-8000-000000000001', null,
        test.wine_row('aaaaaaaa-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 'Château Margaux')),
    test.op('00000000-0000-4000-8000-0000000000f4', 'movement', '44444444-0000-4000-8000-000000000001', null,
        test.movement_row('aaaaaaaa-0000-4000-8000-000000000001', '44444444-0000-4000-8000-000000000001', '33333333-0000-4000-8000-000000000001', 6, 'PURCHASE'))
)) as res;
select test.eq((select res -> 0 from r2), (select res -> 0 from r1), 'replayed op returns the original result');
select test.eq((select count(*) from public.movement)::int, 1, 'no duplicate movement');

-- Optimistic concurrency: update with the right base applies, with a stale base conflicts.
create temp table base_rev as select rev from public.wine where id = '11111111-0000-4000-8000-000000000001';
create temp table r3 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000f5', 'wine', '11111111-0000-4000-8000-000000000001', (select rev from base_rev),
        test.wine_row('aaaaaaaa-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 'Chateau Margaux (edited)'))
)) as res;
select test.eq((select res -> 0 ->> 'status' from r3), 'applied', 'update with current base applies');
create temp table r4 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000f6', 'wine', '11111111-0000-4000-8000-000000000001', (select rev from base_rev),
        test.wine_row('aaaaaaaa-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 'Stale edit'))
)) as res;
select test.eq((select res -> 0 ->> 'status' from r4), 'conflict', 'stale base is a conflict');
select test.eq((select res -> 0 -> 'server_row' ->> 'producer' from r4), 'Chateau Margaux (edited)', 'conflict returns server row');

-- The ledger is append-only, even for the owner.
select test.fails($$update public.movement set delta = 1$$, 'movement update refused');
select test.fails($$delete from public.movement$$, 'movement delete refused');
select test.eq((select count(*) from public.movement)::int, 1, 'ledger unchanged');
select test.fails($$delete from public.wine$$, 'hard delete refused (no delete policy)');

-- A movement that breaks the sign rules is rejected permanently.
create temp table r5 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000f7', 'movement', '44444444-0000-4000-8000-000000000002', null,
        test.movement_row('aaaaaaaa-0000-4000-8000-000000000001', '44444444-0000-4000-8000-000000000002', '33333333-0000-4000-8000-000000000001', 3, 'CONSUME'))
)) as res;
select test.eq((select res -> 0 ->> 'status' from r5), 'rejected', 'invalid movement rejected');
select test.eq((select res -> 0 ->> 'permanent' from r5), 'true', 'validation rejections are permanent');

-- A child whose parent hasn't arrived is retryable, not permanent.
create temp table r6 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000f8', 'lot', '33333333-0000-4000-8000-000000000009', null,
        test.lot_row('aaaaaaaa-0000-4000-8000-000000000001', '33333333-0000-4000-8000-000000000009', '22222222-0000-4000-8000-000000000099'))
)) as res;
select test.eq((select res -> 0 ->> 'permanent' from r6), 'false', 'missing parent is retryable');

-- ============================================================================================
-- Eve (not a member) sees nothing and cannot write.
select test.become('e0000000-0000-4000-8000-000000000004', 'eve@example.com');
select test.eq((select count(*) from public.wine)::int, 0, 'stranger sees no wines');
select test.eq((select count(*) from public.collection)::int, 0, 'stranger sees no collections');
create temp table r7 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000e1', 'wine', '11111111-0000-4000-8000-0000000000e1', null,
        test.wine_row('aaaaaaaa-0000-4000-8000-000000000001', '11111111-0000-4000-8000-0000000000e1', 'Intruder'))
)) as res;
select test.eq((select res -> 0 ->> 'status' from r7), 'rejected', 'stranger push rejected');
select test.fails($$insert into public.collection_member values ('aaaaaaaa-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000004', 'owner')$$,
    'stranger cannot add themselves as a member');
select test.fails($$select public.bootstrap_collection('aaaaaaaa-0000-4000-8000-000000000001', 'Hijack')$$, 'cannot claim an existing collection id');

-- Eve's own collection cannot reference Alice's rows (composite foreign keys).
select public.bootstrap_collection('eeeeeeee-0000-4000-8000-000000000001', 'Eve''s cellar');
create temp table r8 as select public.sync_push('eeeeeeee-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000e2', 'vintage', '22222222-0000-4000-8000-0000000000e2', null,
        test.vintage_row('eeeeeeee-0000-4000-8000-000000000001', '22222222-0000-4000-8000-0000000000e2', '11111111-0000-4000-8000-000000000001', 2015))
)) as res;
select test.eq((select res -> 0 ->> 'status' from r8), 'rejected', 'cross-collection reference rejected');
-- Overwriting another collection's row id is refused.
create temp table r9 as select public.sync_push('eeeeeeee-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000e3', 'wine', '11111111-0000-4000-8000-000000000001', null,
        test.wine_row('eeeeeeee-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 'Overwrite'))
)) as res;
select test.eq((select res -> 0 ->> 'status' from r9), 'rejected', 'cannot take over a row id from another collection');

-- ============================================================================================
-- Households: Alice invites Bob (viewer) and Carol (editor).
select test.become('a0000000-0000-4000-8000-000000000001', 'alice@example.com');
create temp table invites as select
    public.create_invite('aaaaaaaa-0000-4000-8000-000000000001', 'Bob@Example.com', 'viewer') as bob_token,
    public.create_invite('aaaaaaaa-0000-4000-8000-000000000001', 'carol@example.com', 'editor') as carol_token;
grant select on invites to authenticated;
select test.eq((select count(*) from public.collection_invite where token_hash = (select bob_token from invites))::int, 0, 'tokens are stored hashed');

select test.become('e0000000-0000-4000-8000-000000000004', 'eve@example.com');
select test.fails($$select public.accept_invite((select bob_token from invites))$$, 'invite cannot be used by another email');
select test.fails($$select public.create_invite('aaaaaaaa-0000-4000-8000-000000000001', 'eve2@example.com', 'editor')$$, 'non-owner cannot invite');

select test.become('b0000000-0000-4000-8000-000000000002', 'bob@example.com');
select test.eq(public.accept_invite((select bob_token from invites)), 'aaaaaaaa-0000-4000-8000-000000000001'::uuid, 'bob accepts');
select test.fails($$select public.accept_invite((select bob_token from invites))$$, 'invite is single-use');
select test.eq((select count(*) from public.wine)::int, 1, 'viewer can read');
create temp table r10 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000b1', 'wine', '11111111-0000-4000-8000-0000000000b1', null,
        test.wine_row('aaaaaaaa-0000-4000-8000-000000000001', '11111111-0000-4000-8000-0000000000b1', 'Bob tries'))
)) as res;
select test.eq((select res -> 0 ->> 'code' from r10), '42501', 'viewer push rejected');
update public.wine set notes = 'x'; -- RLS: matches no rows for a viewer
select test.eq((select count(*) from public.wine where notes = 'x')::int, 0, 'viewer changed nothing');

select test.become('c0000000-0000-4000-8000-000000000003', 'carol@example.com');
select public.accept_invite((select carol_token from invites));
create temp table r11 as select public.sync_push('aaaaaaaa-0000-4000-8000-000000000001', jsonb_build_array(
    test.op('00000000-0000-4000-8000-0000000000c1', 'movement', '44444444-0000-4000-8000-0000000000c1', null,
        test.movement_row('aaaaaaaa-0000-4000-8000-000000000001', '44444444-0000-4000-8000-0000000000c1', '33333333-0000-4000-8000-000000000001', -1, 'CONSUME'))
)) as res;
select test.eq((select res -> 0 ->> 'status' from r11), 'applied', 'editor can record consumption');
select test.fails($$select public.remove_member('aaaaaaaa-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002')$$, 'editor cannot remove members');

select test.become('a0000000-0000-4000-8000-000000000001', 'alice@example.com');
select public.remove_member('aaaaaaaa-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002');
select test.fails($$select public.remove_member('aaaaaaaa-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001')$$, 'owner cannot be removed');
select test.become('b0000000-0000-4000-8000-000000000002', 'bob@example.com');
select test.eq((select count(*) from public.wine)::int, 0, 'removed member loses access');

-- Anonymous callers see nothing and cannot call RPCs.
reset role;
set role anon;
select set_config('request.jwt.claims', '', false);
select test.fails($$select count(*) from public.wine$$, 'anon has no table access at all');
select test.fails($$select public.my_collections()$$, 'anon cannot call RPCs');

-- ============================================================================================
-- Account deletion (service role via Edge Function): owned collections cascade, memberships go.
reset role;
select test.eq((public.delete_user_data('a0000000-0000-4000-8000-000000000001') -> 'deleted_collections' ->> 0)::uuid,
    'aaaaaaaa-0000-4000-8000-000000000001'::uuid, 'owned collection deleted');
select test.eq((select count(*) from public.movement where collection_id = 'aaaaaaaa-0000-4000-8000-000000000001')::int, 0, 'ledger rows cascaded');
select test.eq((select count(*) from public.collection_member where user_id = 'c0000000-0000-4000-8000-000000000003')::int, 0, 'household memberships removed');
select test.eq((select count(*) from public.collection where id = 'eeeeeeee-0000-4000-8000-000000000001')::int, 1, 'other users untouched');
set role authenticated;
select test.fails($$select public.delete_user_data('e0000000-0000-4000-8000-000000000004')$$, 'users cannot call delete_user_data');
reset role;

\echo 'ALL SERVER TESTS PASSED'
