CREATE TABLE `storage_tbl`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `commodity_code` varchar(255) DEFAULT NULL,
    `cnt`            int(11)      DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY (`commodity_code`)
) DEFAULT CHARSET = UTF8MB4;

INSERT INTO `storage_tbl` (id, commodity_code, cnt)
VALUES (1, '123456', 10);
