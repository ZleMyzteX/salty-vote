create table local_picture_store
(
    id         bigserial primary key,
    local_path text        not null,
    created_at timestamptz not null default now()
);

create table vote_option_external_data
(
    id                      bigserial primary key,
    vote_option_id          bigint      not null references vote_options (id) on delete cascade,
    source_url              text        not null,
    airbnb_title            text        not null,
    airbnb_review_count     numeric     not null,
    airbnb_star_rating      numeric     not null,
    airbnb_picture_url      text        not null,
    airbnb_picture_local_id bigint      not null references local_picture_store (id) on delete cascade,
    raw_payload             jsonb       not null,
    created_at              timestamptz not null default now()
);