-- Minimal stand-in for the parts of Supabase the migrations rely on, so they can be tested on a
-- plain PostgreSQL 16 (CI and local). NOT applied to a real Supabase project.
do $$ begin
    if not exists (select 1 from pg_roles where rolname = 'anon') then create role anon nologin; end if;
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then create role authenticated nologin; end if;
    if not exists (select 1 from pg_roles where rolname = 'service_role') then create role service_role nologin bypassrls; end if;
end $$;

create schema if not exists auth;
create table if not exists auth.users (id uuid primary key, email text);

create or replace function auth.jwt() returns jsonb language sql stable as $$
    select coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb
$$;
create or replace function auth.uid() returns uuid language sql stable as $$
    select nullif(auth.jwt() ->> 'sub', '')::uuid
$$;

create schema if not exists storage;
create table if not exists storage.buckets (id text primary key, name text not null, public boolean default false);
create table if not exists storage.objects (
    id uuid primary key default gen_random_uuid(), bucket_id text references storage.buckets (id), name text, owner uuid
);
alter table storage.objects enable row level security;
create or replace function storage.foldername(name text) returns text[] language sql immutable as $$
    select (string_to_array(name, '/'))[1:array_length(string_to_array(name, '/'), 1) - 1]
$$;

grant usage on schema public, auth, storage to anon, authenticated, service_role;
grant select, insert, update on storage.objects to authenticated;
grant execute on all functions in schema auth to anon, authenticated, service_role;
-- Supabase's default privileges grant table access to these roles; RLS does the real work.
alter default privileges in schema public grant select, insert, update, delete on tables to anon, authenticated, service_role;
