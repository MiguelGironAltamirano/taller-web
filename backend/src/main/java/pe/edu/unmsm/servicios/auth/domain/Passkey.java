package pe.edu.unmsm.servicios.auth.domain;

import java.time.Instant;

/**
 * A WebAuthn credential of a user. {@code publicKey} holds the attested credential data as registered (AAGUID,
 * credential id and COSE public key); no biometric data ever reaches the backend. {@code backedUp} tells that the
 * provider syncs it to other devices.
 */
public record Passkey(Long id, long userId, byte[] credentialId, byte[] publicKey, long signCount, String transports,
		boolean backupEligible, boolean backedUp, String name, Instant createdAt, Instant lastUsedAt) {

	public static final int NAME_MAX = 60;

}
