alter table membership_pass
    add column if not exists payment_id varchar(255);

alter table membership_pass
    drop constraint if exists uk_membership_pass_payment;

alter table membership_pass
    add constraint uk_membership_pass_payment
        unique (payment_id);

alter table membership_pass
    drop constraint if exists fk_membership_pass_payment;

alter table membership_pass
    add constraint fk_membership_pass_payment
        foreign key (payment_id)
            references payment (id);
