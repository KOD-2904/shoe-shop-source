CREATE TABLE IF NOT EXISTS shipping_provider_settings (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    mode VARCHAR(30) NOT NULL,
    mock_fixed_fee INT NOT NULL DEFAULT 30000,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_shipping_provider_settings_provider UNIQUE (provider),
    CONSTRAINT ck_shipping_provider_settings_mode CHECK (mode IN ('REAL', 'MOCK_TEST')),
    CONSTRAINT ck_shipping_provider_settings_mock_fee CHECK (mock_fixed_fee >= 0)
);
