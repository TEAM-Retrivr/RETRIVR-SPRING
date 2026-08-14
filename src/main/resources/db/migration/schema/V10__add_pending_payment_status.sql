alter table payment drop constraint if exists payment_status_check;

alter table payment add constraint payment_status_check
    check ((status)::text = any (
        (array [
            'PENDING'::character varying,
            'UNKNOWN'::character varying,
            'SUCCESS'::character varying,
            'COMPENSATION_REQUIRED'::character varying,
            'REFUND_PROCESSING'::character varying,
            'REFUND_UNKNOWN'::character varying,
            'REFUND_FAILED'::character varying,
            'REFUNDED'::character varying,
            'FAILED'::character varying,
            'SCHEDULE_PENDING'::character varying,
            'SCHEDULE_UNKNOWN'::character varying,
            'SCHEDULED'::character varying,
            'SCHEDULE_CANCELED'::character varying
        ])::text[]
    ));
