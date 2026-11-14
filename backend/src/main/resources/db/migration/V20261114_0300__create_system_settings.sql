-- Global system settings managed by administration (GET/PUT /api/v1/admin/config).
-- Single typed row (id = 1): each setting is a real column with its own type and constraint,
-- so adding a setting is an explicit migration instead of an untyped key/value entry.
CREATE TABLE system_settings (
  id TINYINT NOT NULL PRIMARY KEY,
  auto_close_hours INT NOT NULL,
  email_notifications BOOLEAN NOT NULL,
  triage_suggestions BOOLEAN NOT NULL,
  updated_by BIGINT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT ck_system_settings_single_row CHECK (id = 1),
  CONSTRAINT ck_system_settings_auto_close_hours CHECK (auto_close_hours > 0),
  CONSTRAINT fk_system_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);

INSERT INTO system_settings (id, auto_close_hours, email_notifications, triage_suggestions, updated_by, updated_at)
VALUES (1, 72, FALSE, TRUE, NULL, UTC_TIMESTAMP(6));
