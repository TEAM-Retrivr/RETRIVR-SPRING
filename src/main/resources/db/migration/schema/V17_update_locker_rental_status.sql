alter table locker_rental drop constraint ck_locker_rental_status;

update locker_rental set status = 'APPROVED' where status = 'RENTED';

alter table locker_rental add constraint ck_locker_rental_status
    check (status in ('REQUESTED', 'APPROVED', 'REJECTED', 'RETURNED'));