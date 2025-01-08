DROP TABLE IF EXISTS apzda_mq_mailbox;
CREATE TABLE apzda_mq_mailbox
(
    id            BIGINT UNSIGNED                                     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    create_time   datetime                                            NULL     DEFAULT NULL,
    update_time   datetime                                            NULL     DEFAULT NULL,
    next_retry_at BIGINT UNSIGNED                                     NOT NULL DEFAULT 0,
    transactional bit                                                 not null default false comment 'Transactional Message',
    mail_id       VARCHAR(64)                                         NULL     DEFAULT NULL COMMENT 'ID',
    status        ENUM ('PENDING','SENDING','SENT','FAIL','RETRYING') NOT NULL DEFAULT 'PENDING' COMMENT 'Status',
    recipients    text                                                null comment 'Recipients',
    post_time     BIGINT UNSIGNED                                     null     DEFAULT null comment 'Delivery time',
    content       Longtext                                            NOT NULL COMMENT 'Content of this message',
    properties    Longtext                                            NULL COMMENT 'Properties',
    msg_id        varchar(64)                                         NULL comment 'Message Id',
    content_type  varchar(128)                                        NULL COMMENT 'the class name of the content',
    retries       SMALLINT UNSIGNED                                   NOT NULL DEFAULT 0 COMMENT 'The count ',
    remark        text                                                NULL COMMENT 'remark',
    INDEX IDX_STATUS (status, next_retry_at),
    INDEX IDX_CT (create_time),
    INDEX IDX_MSG_ID (msg_id),
    INDEX IDX_MAIL_ID (mail_id)
) COMMENT 'RocketMQ mailbox';
