ALTER TABLE payments
    ADD COLUMN vnp_txn_ref VARCHAR(64) NULL;

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_vnp_txn_ref UNIQUE (vnp_txn_ref);
