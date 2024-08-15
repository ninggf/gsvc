CREATE TABLE `account_tbl`
(
    `id`      int(11) NOT NULL AUTO_INCREMENT,
    `user_id` varchar(255)     DEFAULT NULL,
    `money`   int(11) unsigned DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY UDX_USER_ID (user_id)
) DEFAULT CHARSET = utf8mb4;

INSERT INTO `account_tbl` (id, user_id, money)
VALUES (1, '654321', 100);
