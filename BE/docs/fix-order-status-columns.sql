-- Fix legacy MySQL enum/short columns for order statuses.
-- Current Java enums are stored as strings and need to accept:
-- OrderStatus: PENDING, CONFIRMED, PACKING, READY_TO_SHIP, SHIPPING, DELIVERED, CANCELLED, FAILED, RETURNED
-- ShippingStatus: NOT_CREATED, CREATED, PICKING, PICKED, DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNING, RETURNED, CANCELLED

ALTER TABLE orders
    MODIFY COLUMN status VARCHAR(30) NOT NULL,
    MODIFY COLUMN shipping_status VARCHAR(30) NOT NULL;
