create index idx_item_unit_item_id
    on item_unit (item_id);

create index idx_item_unit_active_item_id
    on item_unit (item_id)
    where deleted_at is null;

alter table item_unit
    drop constraint item_unit_item_id_label_key;
