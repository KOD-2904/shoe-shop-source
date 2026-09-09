CREATE TABLE IF NOT EXISTS voucher_usages (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    voucher_id VARCHAR(255) NOT NULL,
    voucher_code VARCHAR(64) NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    released_at DATETIME,
    CONSTRAINT uk_voucher_usage_order_code UNIQUE (order_id, voucher_code),
    CONSTRAINT fk_voucher_usages_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id)
);

CREATE INDEX idx_voucher_usages_voucher_code ON voucher_usages(voucher_code);
CREATE INDEX idx_voucher_usages_order_id ON voucher_usages(order_id);
CREATE INDEX idx_voucher_usages_user_id ON voucher_usages(user_id);
