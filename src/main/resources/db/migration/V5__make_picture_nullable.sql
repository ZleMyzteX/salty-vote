-- Make airbnb_picture_local_id nullable to support external data without pictures
alter table vote_option_external_data
    alter column airbnb_picture_local_id drop not null;

