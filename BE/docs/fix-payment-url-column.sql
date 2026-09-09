-- VNPay payment URLs can exceed the old VARCHAR length.
ALTER TABLE payments
    MODIFY COLUMN payment_url TEXT NULL;
