package pe.edu.unmsm.servicios.auth.domain;

import java.time.Instant;

public record RefreshToken(Long id, long userId, String tokenHash, Instant expiresAt, Instant revokedAt) {

	public boolean isUsableAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

}
