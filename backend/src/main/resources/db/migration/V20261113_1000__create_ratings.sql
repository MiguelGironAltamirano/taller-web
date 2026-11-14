CREATE TABLE ratings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  score TINYINT NOT NULL,
  comment VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_ratings_request FOREIGN KEY (request_id) REFERENCES service_requests (id) ON DELETE CASCADE,
  CONSTRAINT uq_ratings_request UNIQUE (request_id),
  CONSTRAINT chk_ratings_score CHECK (score BETWEEN 1 AND 5)
);
