create table vote_collaborators
(
    id              bigserial primary key,
    vote_id         bigint not null references votes (id) on delete cascade,
    user_id         uuid not null references users (uuid),
    created_at      timestamptz not null default now(),
    constraint uq_vote_collaborator unique (vote_id, user_id)
);

-- Add index for faster lookups
create index idx_vote_collaborators_vote_id on vote_collaborators(vote_id);
create index idx_vote_collaborators_user_id on vote_collaborators(user_id);

