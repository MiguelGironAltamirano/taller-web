-- Draft of a new request (docs/api/contrato-v1.md «Borrador de nueva solicitud»): one per requester, kept on the
-- server so it follows the person across devices and never stays behind on a shared computer. The fields are the
-- form values as typed (names, not ids), because a draft may be half filled.
CREATE TABLE request_drafts (
  user_id BIGINT PRIMARY KEY,
  category VARCHAR(150) NOT NULL DEFAULT '',
  description VARCHAR(2000) NOT NULL DEFAULT '',
  priority VARCHAR(150) NOT NULL DEFAULT '',
  faculty VARCHAR(150) NOT NULL DEFAULT '',
  pavilion VARCHAR(150) NOT NULL DEFAULT '',
  room VARCHAR(150) NOT NULL DEFAULT '',
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_request_drafts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
