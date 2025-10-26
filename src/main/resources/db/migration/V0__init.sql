CREATE EXTENSION if not exists citext;

create table if not exists users
(
    uuid       uuid         not null primary key,
    email      citext       not null,
    username   varchar(50)  not null,
    password   varchar(500) not null,
    admin      boolean      not null,
    enabled    boolean      not null,

    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now(),

    constraint uq_users_email unique (email),
    constraint uq_users_username unique (username)
);

create type action_type as enum (
    -- lobby
    'LOBBY_CREATE',
    'LOBBY_JOIN',
    'LOBBY_CANCEL',
    'LOBBY_RESOLVE',

    -- user
    'USER_REGISTERED',
    'USER_LOGIN',
    'USER_PASSWORD_CHANGED'
    );

create table history
(
    id          bigserial primary key,
    created_at  timestamptz not null default now(),

    action_type action_type not null,

    user_id     uuid        null references users (uuid),

    event_data  jsonb       null
);

create or replace function prevent_update_delete()
    returns trigger as
$$
begin
    raise exception 'history is insert-only';
end;
$$ language plpgsql;

create trigger prevent_update_delete
    before update or delete
    on history
    for each row
execute function prevent_update_delete();