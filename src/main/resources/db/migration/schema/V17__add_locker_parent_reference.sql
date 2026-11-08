-- Schema-only migration: locker must be empty before this migration is applied.
-- Existing lockers need a parent assignment before NOT NULL can be enforced;
-- no backfill is included as requested.
-- Each representative points to itself; other cells point to their representative.

alter table locker
    add column parent_locker uuid not null;

alter table locker
    add constraint fk_locker_parent
        foreign key (parent_locker) references locker (id);

create index idx_locker_parent
    on locker (parent_locker);

alter table locker
    drop column row_span,
    drop column column_span;
