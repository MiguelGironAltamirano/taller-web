CREATE TABLE evidences (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  uploader_id BIGINT NOT NULL,
  file_name VARCHAR(500) NOT NULL,
  stored_name CHAR(36) NOT NULL,
  kind VARCHAR(5) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_evidences_request FOREIGN KEY (request_id) REFERENCES service_requests (id) ON DELETE CASCADE,
  CONSTRAINT fk_evidences_uploader FOREIGN KEY (uploader_id) REFERENCES users (id),
  CONSTRAINT ck_evidences_kind CHECK (kind IN ('IMG', 'PDF'))
);

CREATE INDEX ix_evidences_request ON evidences (request_id);
