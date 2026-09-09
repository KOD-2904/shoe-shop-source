-- Fix legacy MySQL enum columns for payments.
-- Current Java enums are stored as strings and need to accept:
-- PaymentStatus: UNPAID, PAID, FAILED, REFUNDED
-- PaymentMethod: COD, VNPAY, MOMO, STRIPE

ALTER TABLE payments
    MODIFY COLUMN status VARCHAR(20) NOT NULL,
    MODIFY COLUMN method VARCHAR(20) NOT NULL;
