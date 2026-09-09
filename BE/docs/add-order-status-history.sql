CREATE TABLE IF NOT EXISTS order_status_history (
  id VARCHAR(255) NOT NULL PRIMARY KEY,
  order_id VARCHAR(255) NOT NULL,
  actor_id VARCHAR(255),
  old_order_status VARCHAR(40),
  new_order_status VARCHAR(40),
  old_payment_status VARCHAR(40),
  new_payment_status VARCHAR(40),
  old_shipping_status VARCHAR(40),
  new_shipping_status VARCHAR(40),
  source VARCHAR(64) NOT NULL,
  note VARCHAR(1000),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_status_history_actor FOREIGN KEY (actor_id) REFERENCES user_account(id)
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);
CREATE INDEX idx_order_status_history_actor_id ON order_status_history(actor_id);
