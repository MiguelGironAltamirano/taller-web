CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_id BIGINT NOT NULL,
  request_id BIGINT NOT NULL,
  text VARCHAR(500) NOT NULL,
  read_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
  CONSTRAINT fk_notifications_request FOREIGN KEY (request_id) REFERENCES service_requests (id) ON DELETE CASCADE
);
CREATE INDEX ix_notifications_recipient_read ON notifications (recipient_id, read_at);
