CREATE TABLE areas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_areas_name UNIQUE (name)
);

CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  default_area_id BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_categories_name UNIQUE (name),
  CONSTRAINT fk_categories_area FOREIGN KEY (default_area_id) REFERENCES areas (id)
);

CREATE TABLE priorities (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  sla_hours INT NOT NULL,
  sort_order INT NOT NULL,
  CONSTRAINT uk_priorities_name UNIQUE (name),
  CONSTRAINT ck_priorities_sla CHECK (sla_hours > 0)
);

CREATE TABLE locations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  kind VARCHAR(20) NOT NULL,
  parent_id BIGINT NULL,
  CONSTRAINT fk_locations_parent FOREIGN KEY (parent_id) REFERENCES locations (id),
  CONSTRAINT ck_locations_kind CHECK (kind IN ('FACULTAD', 'PABELLON', 'AMBIENTE'))
);

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  requester_type VARCHAR(20) NULL,
  area_id BIGINT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email),
  CONSTRAINT fk_users_area FOREIGN KEY (area_id) REFERENCES areas (id),
  CONSTRAINT ck_users_role CHECK (role IN ('SOLICITANTE', 'TECNICO', 'SUPERVISOR', 'ADMINISTRADOR')),
  CONSTRAINT ck_users_requester_type CHECK (requester_type IS NULL OR requester_type IN ('ESTUDIANTE', 'DOCENTE', 'ADMINISTRATIVO'))
);

CREATE TABLE refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE request_code_sequences (
  seq_year INT PRIMARY KEY,
  `last_value` INT NOT NULL
);

CREATE TABLE service_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  requester_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  description VARCHAR(2000) NOT NULL,
  priority_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  area_id BIGINT NOT NULL,
  technician_id BIGINT NULL,
  closure_reason VARCHAR(20) NULL,
  created_at DATETIME(6) NOT NULL,
  due_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_service_requests_code UNIQUE (code),
  CONSTRAINT fk_service_requests_requester FOREIGN KEY (requester_id) REFERENCES users (id),
  CONSTRAINT fk_service_requests_category FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT fk_service_requests_location FOREIGN KEY (location_id) REFERENCES locations (id),
  CONSTRAINT fk_service_requests_priority FOREIGN KEY (priority_id) REFERENCES priorities (id),
  CONSTRAINT fk_service_requests_area FOREIGN KEY (area_id) REFERENCES areas (id),
  CONSTRAINT fk_service_requests_technician FOREIGN KEY (technician_id) REFERENCES users (id),
  CONSTRAINT ck_service_requests_status CHECK (status IN ('REGISTRADA', 'EN_EVALUACION', 'ASIGNADA', 'EN_ATENCION', 'RESUELTA', 'CERRADA')),
  CONSTRAINT ck_service_requests_closure CHECK (closure_reason IS NULL OR closure_reason IN ('NO_PROCEDE', 'DUPLICADA', 'CONFIRMADA'))
);

CREATE INDEX ix_service_requests_requester ON service_requests (requester_id);
CREATE INDEX ix_service_requests_area_status ON service_requests (area_id, status);
CREATE INDEX ix_service_requests_technician_status ON service_requests (technician_id, status);

CREATE TABLE request_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  from_status VARCHAR(20) NULL,
  to_status VARCHAR(20) NOT NULL,
  actor_id BIGINT NOT NULL,
  technician_id BIGINT NULL,
  reason VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_request_history_request FOREIGN KEY (request_id) REFERENCES service_requests (id),
  CONSTRAINT fk_request_history_actor FOREIGN KEY (actor_id) REFERENCES users (id),
  CONSTRAINT fk_request_history_technician FOREIGN KEY (technician_id) REFERENCES users (id)
);

CREATE INDEX ix_request_history_request ON request_history (request_id, created_at);
