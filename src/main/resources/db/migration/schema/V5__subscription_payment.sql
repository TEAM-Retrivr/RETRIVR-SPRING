alter table subscription alter column plan drop not null;

alter table subscription drop constraint if exists subscription_status_check;
alter table subscription add constraint subscription_status_check
    check ((status)::text = any (
        (array [
            'ACTIVE'::character varying,
            'CANCELED'::character varying,
            'PAST_DUE'::character varying,
            'PAYMENT_FAILED'::character varying
        ])::text[]
    ));

alter table subscription add column if not exists payment_fail_count bigint not null default 0;
alter table subscription drop column if exists billing_key;
alter table subscription add column if not exists payment_schedule_id varchar(255);

create table if not exists payment_method
(
    created_at      timestamp(6) not null,
    disabled_at     timestamp(6),
    is_default      boolean      not null default false,
    registered_at   timestamp(6) not null,
    updated_at      timestamp(6) not null,
    organization_id bigint       not null
        constraint uk_payment_method_organization
            unique
        constraint fk_payment_method_organization
            references organization,
    billing_key     varchar(255) not null,
    id              varchar(255) not null
        primary key,
    provider        varchar(255) not null
        constraint payment_method_provider_check
            check ((provider)::text = any (
                (array [
                    'MOCK'::character varying,
                    'TOSS'::character varying,
                    'KAKAOPAY'::character varying,
                    'CARD'::character varying
                ])::text[]
            )),
    status          varchar(255) not null
        constraint payment_method_status_check
            check ((status)::text = any (
                (array [
                    'ACTIVE'::character varying,
                    'DISABLED'::character varying
                ])::text[]
            ))
);

alter table subscription add column if not exists payment_method_id varchar(255);
alter table subscription drop constraint if exists uk_subscription_payment_method;
alter table subscription add constraint uk_subscription_payment_method unique (payment_method_id);
alter table subscription drop constraint if exists fk_subscription_payment_method;
alter table subscription add constraint fk_subscription_payment_method
    foreign key (payment_method_id)
        references payment_method;

create table if not exists payment
(
    amount               bigint       not null,
    canceled_at          timestamp(6),
    created_at           timestamp(6) not null,
    failed_at            timestamp(6),
    paid_at              timestamp(6),
    scheduled_at         timestamp(6),
    updated_at           timestamp(6) not null,
    organization_id      bigint       not null
        constraint fk_payment_organization
            references organization,
    failure_code         varchar(255),
    failure_reason       varchar(255),
    id                   varchar(255) not null
        primary key,
    port_one_schedule_id  varchar(255),
    provider             varchar(255) not null
        constraint payment_provider_check
            check ((provider)::text = any (
                (array [
                    'MOCK'::character varying,
                    'TOSS'::character varying,
                    'KAKAOPAY'::character varying,
                    'CARD'::character varying
                ])::text[]
            )),
    provider_payment_key varchar(255),
    plan                 varchar(255) not null
        constraint payment_plan_check
            check ((plan)::text = any (
                (array [
                    'MONTHLY'::character varying,
                    'YEARLY'::character varying
                ])::text[]
            )),
    status               varchar(255) not null
        constraint payment_status_check
            check ((status)::text = any (
                (array [
                    'SUCCESS'::character varying,
                    'FAILED'::character varying,
                    'SCHEDULED'::character varying,
                    'SCHEDULE_CANCELED'::character varying
                ])::text[]
            ))
);

create index if not exists idx_payment_organization
    on payment (organization_id);

create index if not exists idx_payment_status_scheduled_at
    on payment (status, scheduled_at);
