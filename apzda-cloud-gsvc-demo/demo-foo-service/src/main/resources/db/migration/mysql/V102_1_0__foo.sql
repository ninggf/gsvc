CREATE TABLE `order_tbl`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `user_id`        varchar(255) DEFAULT NULL,
    `commodity_code` varchar(255) DEFAULT NULL,
    `cnt`            int(11)      DEFAULT 0,
    `money`          int(11)      DEFAULT 0,
    PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8mb4;
