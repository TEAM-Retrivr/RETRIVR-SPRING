create table coupon
(
    active_start_at date,
    duration_days   integer      not null
        constraint coupon_duration_days_positive
            check (duration_days > 0),
    expires_at      date,
    total_quantity  integer      not null
        constraint coupon_total_quantity_range
            check (total_quantity >= 0),
    used_quantity   integer      not null
        constraint coupon_used_quantity_range
            check (used_quantity >= 0),
    created_at      timestamp(6) not null,
    updated_at      timestamp(6) not null,
    code            varchar(255) not null
        constraint uk_coupon_code
            unique,
    description     varchar(255) not null,
    guideline       varchar(255) not null,
    id              varchar(255) not null
        primary key,
    name            varchar(255) not null,
    status          varchar(255) not null
        constraint coupon_status_check
            check ((status)::text = ANY
        ((ARRAY ['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))
);

create table subscription
(
    canceled_at       timestamp(6),
    created_at        timestamp(6) not null,
    next_billing_at   timestamp(6),
    organization_id   bigint       not null
        constraint uk_subscription_org
            unique
        constraint fk5quo13ysitiufm8unlaume479
            references organization,
    payment_failed_at timestamp(6),
    started_at        timestamp(6),
    updated_at        timestamp(6) not null,
    id                varchar(255) not null
        primary key,
    plan              varchar(255) not null
        constraint subscription_plan_check
            check ((plan)::text = ANY
        ((ARRAY ['MONTHLY'::character varying, 'YEARLY'::character varying])::text[])),
    status            varchar(255) not null
        constraint subscription_status_check
            check ((status)::text = ANY
                   ((ARRAY ['ACTIVE'::character varying, 'CANCELED'::character varying, 'PAYMENT_FAILED'::character varying])::text[]))
);


create table coupon_registration
(
    created_at      timestamp(6) not null,
    organization_id bigint       not null
        constraint fkp33fe8n6bxkw5nuuwh88g2k8e
            references organization,
    registered_at   timestamp(6),
    updated_at      timestamp(6) not null,
    coupon_id       varchar(255) not null
        constraint fkd4q0k3t43xlua9ypy4dr73rw
            references coupon,
    id              varchar(255) not null
        primary key,
    constraint uk_coupon_registration_org_coupon
        unique (organization_id, coupon_id)
);

create table membership_pass
(
    end_at                 timestamp(6) not null,
    organization_id        bigint       not null
        constraint fkijlj16nx6c4sal2w831644uij
            references organization,
    sequence               bigint       not null,
    start_at               timestamp(6) not null,
    coupon_registration_id varchar(255)
        unique
        constraint fka7ry0af437yrh33phc5krfwit
            references coupon_registration,
    id                     varchar(255) not null
        primary key,
    level                  varchar(255)
        constraint membership_pass_level_check
            check ((level)::text = ANY
        ((ARRAY ['PREMIUM'::character varying, 'FREE'::character varying])::text[])),
    source_type            varchar(255) not null
        constraint membership_pass_source_type_check
            check ((source_type)::text = ANY
                   ((ARRAY ['SUBSCRIPTION'::character varying, 'COUPON'::character varying])::text[])),
    status                 varchar(255) not null
        constraint membership_pass_status_check
            check ((status)::text = ANY
                   ((ARRAY ['REGISTERED'::character varying, 'ACTIVE'::character varying, 'EXPIRED'::character varying])::text[])),
    subscription_id        varchar(255)
        constraint fk5lu2tyhh22qp130eik94jp1pg
            references subscription,

        constraint membership_pass_source_fk_check
            check (
                (source_type = 'COUPON' and coupon_registration_id is not null and subscription_id is null)
                or
                (source_type = 'SUBSCRIPTION' and subscription_id is not null and coupon_registration_id is null)
            )
);