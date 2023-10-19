create table if not exists t_users
(
    uid   varchar(32) not null primary key,
    name  varchar(32) not null,
    ver   bigint      not null default 0,
    del   tinyint     not null default 0,
    roles varchar(1023)
);


create table if not exists t_roles
(
    rid  varchar(32)    not null primary key,
    name varchar(32)    not null,
    del  tinyint        not null default 0,
    dd   decimal(18, 6) not null default 0.000000
);