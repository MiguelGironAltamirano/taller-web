package pe.edu.unmsm.servicios.auth.domain;

import java.time.Instant;

/** One refresh token of a session; rotating it issues a new token with the same {@code sessionId}. */
public record RefreshToken(Long id, long userId, String sessionId, String tokenHash, Instant expiresAt,
		Instant revokedAt) {

	public boolean isUsableAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

}
