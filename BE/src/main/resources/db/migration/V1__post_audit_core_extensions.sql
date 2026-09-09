CREATE TABLE IF NOT EXISTS permission (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permission (
    role_id VARCHAR(255) NOT NULL,
    permission_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_discounts (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(255) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(18, 2) NOT NULL,
    max_discount_amount DECIMAL(18, 2) NULL,
    active BIT NOT NULL,
    starts_at DATETIME(6) NULL,
    ends_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_product_discounts_target (target_type, target_id),
    INDEX idx_product_discounts_active_window (active, starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS voucher_usages (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    voucher_id VARCHAR(255) NOT NULL,
    voucher_code VARCHAR(64) NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NULL,
    released_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_voucher_usage_order_code UNIQUE (order_id, voucher_code),
    CONSTRAINT fk_voucher_usages_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    INDEX idx_voucher_usages_order_id (order_id),
    INDEX idx_voucher_usages_user_id (user_id),
    INDEX idx_voucher_usages_voucher_code (voucher_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_review_images (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    review_id VARCHAR(255) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_review_images_review FOREIGN KEY (review_id) REFERENCES product_reviews(id),
    INDEX idx_product_review_images_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64),
    IN column_definition_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND COLUMN_NAME = column_name_param
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', table_name_param, ' ADD COLUMN ', column_name_param, ' ', column_definition_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE add_index_if_missing(
    IN table_name_param VARCHAR(64),
    IN index_name_param VARCHAR(64),
    IN index_definition_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND INDEX_NAME = index_name_param
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', table_name_param, ' ADD ', index_definition_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CALL add_column_if_missing('product_reviews', 'order_item_id', 'VARCHAR(255) NULL')//
CALL add_column_if_missing('orders', 'voucher_usage_counted', 'BIT NOT NULL DEFAULT 0')//
CALL add_column_if_missing('orders', 'voucher_usage_released_at', 'DATETIME(6) NULL')//
CALL add_index_if_missing('product_reviews', 'idx_product_reviews_order_item_id', 'INDEX idx_product_reviews_order_item_id (order_item_id)')//
CALL add_index_if_missing('product_reviews', 'uk_product_review_order_item', 'UNIQUE INDEX uk_product_review_order_item (order_item_id)')//
CALL add_index_if_missing('orders', 'idx_orders_shipping_order_code', 'INDEX idx_orders_shipping_order_code (shipping_order_code)')//

DROP PROCEDURE add_column_if_missing//
DROP PROCEDURE add_index_if_missing//

DELIMITER ;
