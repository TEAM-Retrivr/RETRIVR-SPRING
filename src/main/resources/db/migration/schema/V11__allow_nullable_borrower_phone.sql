alter table borrower
    alter column phone drop not null;

alter table borrower
    add constraint borrower_contact_check
        check ((phone is not null) <> (email is not null));
