package pe.edu.unmsm.servicios.auth.application;

import java.time.Instant;
import java.util.Optional;

import pe.edu.unmsm.servicios.auth.domain.ChallengePurpose;

/** Challenges are stored, not kept in memory, so any instance of the backend can finish a ceremony. */
public interface WebAuthnChallengeRepository {

	void save(String id, ChallengePurpose purpose, Long userId, byte[] challenge, Instant expiresAt);

	/**
	 * Marks the challenge as used and returns it, atomically, only when it exists, serves this purpose (and user, for
	 * registrations), has not expired and was not used before. Otherwise empty, and it can never be used again.
	 */
	Optional<byte[]> consume(String id, ChallengePurpose purpose, Long userId, Instant now);

	void deleteExpired(Instant now);

}
