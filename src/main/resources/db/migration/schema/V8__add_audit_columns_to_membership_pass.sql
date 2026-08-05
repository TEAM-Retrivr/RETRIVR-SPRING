alter table membership_pass
    add column if not exists created_at timestamp(6) not null default current_timestamp,
    add column if not exists updated_at timestamp(6) not null default current_timestamp;

alter table membership_pass
    alter column created_at drop default,
    alter column updated_at drop default;
