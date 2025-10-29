create type vote_type as enum ('RANKING', 'SIMPLE');

create table votes
(
    id              bigserial primary key,
    title           text not null,
    description     text null,
    creator_id      uuid not null references users (uuid),
    vote_type       vote_type not null,
    allow_multi     boolean not null default false, -- relevant for simple polls
    allow_anonymous boolean not null default false,
    done            boolean not null default false,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create table vote_options
(
    id          bigserial primary key,
    vote_id     bigint not null references votes (id) on delete cascade,
    label       text not null,
    data        jsonb null,
    created_at  timestamptz not null default now()
);

create table vote_submissions
(
    id          bigserial primary key,
    vote_id     bigint not null references votes (id) on delete cascade,
    user_id     uuid not null references users (uuid),
    created_at  timestamptz not null default now(),
    unique (vote_id, user_id) -- prevent multiple submissions
);

create table vote_submission_entries
(
    id              bigserial primary key,
    submission_id   bigint not null references vote_submissions (id) on delete cascade,
    option_id       bigint not null references vote_options (id) on delete cascade,
    rank            int null,
    selected        boolean null,
    constraint uq_submission_option unique (submission_id, option_id)
);