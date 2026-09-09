ALTER TABLE product_reviews
    ADD COLUMN order_item_id VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_product_review_order_item ON product_reviews(order_item_id);
CREATE INDEX idx_product_reviews_order_item_id ON product_reviews(order_item_id);

ALTER TABLE product_reviews
    ADD CONSTRAINT fk_product_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id);

CREATE TABLE IF NOT EXISTS product_review_images (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    review_id VARCHAR(255) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    CONSTRAINT fk_product_review_images_review FOREIGN KEY (review_id) REFERENCES product_reviews(id)
);

CREATE INDEX idx_product_review_images_review_id ON product_review_images(review_id);
