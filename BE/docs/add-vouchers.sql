CREATE TABLE IF NOT EXISTS vouchers (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(18, 2) NOT NULL,
    min_order_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    max_discount_amount DECIMAL(18, 2),
    usage_limit INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at DATETIME,
    ends_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_vouchers_code UNIQUE (code),
    CONSTRAINT ck_vouchers_type CHECK (type IN ('FIXED', 'PERCENT')),
    CONSTRAINT ck_vouchers_value CHECK (discount_value > 0),
    CONSTRAINT ck_vouchers_usage_limit CHECK (usage_limit > 0),
    CONSTRAINT ck_vouchers_used_count CHECK (used_count >= 0)
);

ALTER TABLE shipping_fee_snapshots
    ADD COLUMN discount_amount DECIMAL(18, 2) NULL,
    ADD COLUMN voucher_code VARCHAR(64) NULL;
