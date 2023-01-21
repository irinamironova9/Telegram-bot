-- liquibase formatted sql

-- changeset imironova:1
create table notification_task
(
    id        bigserial primary key,
    chat_id   bigint    not null,
    text      text      not null,
    date_time timestamp not null
);
