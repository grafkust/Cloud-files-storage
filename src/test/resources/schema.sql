
create table if not exists users
(
    id bigint primary key auto_increment,
    username varchar(255) not null unique,
    email varchar(255) not null unique,
    password varchar(255) not null
    );

create table if not exists users_roles
(
    user_id bigint not null,
    role varchar(255) not null,
    primary key(user_id, role),
    foreign key(user_id) references users(id) on delete cascade on update no action
    );