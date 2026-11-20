-- Login with fingerprint or passkey (WebAuthn), docs/api/passkeys-v1.md. Only public keys are stored: the fingerprint
-- never leaves the user's device.

-- Opaque WebAuthn user handle (64 random bytes), created with the user's first passkey. Never the id or the email.
ALTER TABLE users ADD COLUMN webauthn_user_handle VARBINARY(64) NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_webauthn_user_handle UNIQUE (webauthn_user_handle);

CREATE TABLE passkeys (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  credential_id VARBINARY(1023) NOT NULL,
  -- Attested credential data as registered (AAGUID, credential id and COSE public key).
  public_key VARBINARY(2048) NOT NULL,
  sign_count BIGINT NOT NULL,
  transports VARCHAR(100) NULL,
  backup_eligible BOOLEAN NOT NULL,
  backed_up BOOLEAN NOT NULL,
  name VARCHAR(60) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  last_used_at DATETIME(6) NULL,
  CONSTRAINT uk_passkeys_credential UNIQUE (credential_id),
  CONSTRAINT fk_passkeys_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_passkeys_user ON passkeys (user_id, created_at);

-- One-use challenges of the registration and login ceremonies; expired ones are removed when new ones are issued.
CREATE TABLE webauthn_challenges (
  id CHAR(36) PRIMARY KEY,
  purpose VARCHAR(12) NOT NULL,
  user_id BIGINT NULL,
  challenge BINARY(32) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL,
  CONSTRAINT ck_webauthn_challenges_purpose CHECK (purpose IN ('LOGIN', 'REGISTRATION'))
);

CREATE INDEX ix_webauthn_challenges_expires ON webauthn_challenges (expires_at);
