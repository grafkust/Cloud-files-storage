--liquibase formatted sql
--changeset tasklist:1

create table if not exists users
(
    id       bigint primary key auto_increment,
    name     varchar(255) not null,
    username varchar(255) not null unique,
    password varchar(255) not null
);

create table if not exists tasks
(
    id              bigint primary key auto_increment,
    title           varchar(255) null,
    description     varchar(255) null,
    status          varchar(255) not null,
    expiration_date timestamp    null
);


