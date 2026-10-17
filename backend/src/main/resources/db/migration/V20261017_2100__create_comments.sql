CREATE TABLE comments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  text VARCHAR(1000) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_comments_request FOREIGN KEY (request_id) REFERENCES service_requests (id),
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX ix_comments_request ON comments (request_id, created_at);
