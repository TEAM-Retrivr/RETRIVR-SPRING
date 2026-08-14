alter table subscription drop constraint if exists subscription_status_check;

alter table subscription add constraint subscription_status_check
    check ((status)::text = any (
        (array [
            'START_PENDING'::character varying,
            'ACTIVE'::character varying,
            'CANCELED'::character varying,
            'PAYMENT_FAILED'::character varying
        ])::text[]
    ));
