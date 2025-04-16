create table t_roles
(
    rid  varchar(32)    not null primary key,
    name varchar(32)    not null,
    del  tinyint        not null default 0,
    dd   decimal(18, 6) not null default 0.000000,
    ver  smallint       not null default 0
);
