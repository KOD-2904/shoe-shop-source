CREATE TABLE IF NOT EXISTS product_discounts (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    name VARCHAR(255) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(18, 2) NOT NULL,
    max_discount_amount DECIMAL(18, 2),
    active BIT NOT NULL,
    starts_at DATETIME,
    ends_at DATETIME
);

CREATE INDEX idx_product_discounts_target ON product_discounts(target_type, target_id);
CREATE INDEX idx_product_discounts_active_window ON product_discounts(active, starts_at, ends_at);
