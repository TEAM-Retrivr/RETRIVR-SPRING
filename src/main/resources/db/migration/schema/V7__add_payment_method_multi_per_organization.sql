alter table payment_method drop constraint if exists uk_payment_method_organization;

alter table payment_method add column if not exists is_default boolean not null default false;

update payment_method
set is_default = true
where id in (
    select distinct on (organization_id) id
    from payment_method
    where status = 'ACTIVE'
    order by organization_id, registered_at asc
);

create index if not exists idx_payment_method_organization
    on payment_method (organization_id);

alter table subscription drop constraint if exists fk_subscription_payment_method;
alter table subscription add constraint fk_subscription_payment_method
    foreign key (payment_method_id)
        references payment_method;
