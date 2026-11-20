-- One open session per user (docs/api/contrato-v1.md «Una sola sesión por usuario»).
-- A session is born at login and survives refresh-token rotation, so every token of it shares session_id;
-- the access token carries it as the `sid` claim and each request checks that the session is still open.
ALTER TABLE refresh_tokens ADD COLUMN session_id CHAR(36) NULL;

-- Tokens issued before this change become one session each; the first refresh carries them over.
UPDATE refresh_tokens SET session_id = UUID() WHERE session_id IS NULL;

ALTER TABLE refresh_tokens MODIFY session_id CHAR(36) NOT NULL;

CREATE INDEX ix_refresh_tokens_session ON refresh_tokens (session_id, revoked_at, expires_at);
