-- noinspection SqlNoDataSourceInspectionForFile
create table if not exists t_users
(
    uid         varchar(32) not null primary key,
    created_at  BIGINT               DEFAULT 0,
    created_by  VARCHAR(32)          DEFAULT NULL,
    updated_at  BIGINT               DEFAULT 0,
    updated_by  VARCHAR(32)          DEFAULT NULL,
    merchant_id VARCHAR(32)          DEFAULT NULL,
    name        varchar(32) not null,
    ver         bigint      not null default 0,
    del         tinyint     not null default 0,
    roles       varchar(1023),
    type        varchar(2)  not null
);

DELETE
FROM t_users
WHERE uid <> '';

INSERT INTO t_users(uid, name, roles, type)
values ('1', 'u1', '1', '1'),
       ('2', 'u2', '2', '3'),
       ('3', 'u3', '1', '2');

create table if not exists t_roles
(
    rid  varchar(32) not null primary key,
    name varchar(32) not null,
    del  tinyint     not null default 0,
    type varchar(2)  not null
);
DELETE
FROM t_roles
WHERE rid <> '';
insert into t_roles(rid, name, del, type)
values ('1', 'r1', 0, '1'),
       ('2', 'r2', 0, '3');

create table if not exists sys_dict_item
(
    id         int          not null auto_increment primary key,
    del        tinyint      not null default 0 comment 'delete',
    dict_code  varchar(32)  not null comment 'dictionary code',
    dict_value varchar(64)  not null comment 'dictionary value',
    dict_label varchar(128) not null comment 'the label of this dictionary item'
);

DELETE
FROM sys_dict_item
WHERE id > 0;
insert into sys_dict_item(del, dict_code, dict_value, dict_label)
values (0, 'test', '1', 'Test1'),
       (0, 'test', '2', 'test2'),
       (1, 'test', '3', 'test3');

insert into sys_dict_item(del, dict_code, dict_value, dict_label)
values (0, 'test1', '1', 'Test1'),
       (1, 'test1', '2', 'test2'),
       (0, 'test1', '3', 'test3');
