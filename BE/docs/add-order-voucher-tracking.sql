ALTER TABLE orders
    ADD COLUMN voucher_code VARCHAR(64) NULL,
    ADD COLUMN voucher_usage_counted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN voucher_usage_released_at DATETIME NULL;
