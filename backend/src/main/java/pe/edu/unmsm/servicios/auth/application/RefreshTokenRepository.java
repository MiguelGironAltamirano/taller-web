package pe.edu.unmsm.servicios.auth.application;

import java.time.Instant;
import java.util.Optional;

import pe.edu.unmsm.servicios.auth.domain.RefreshToken;

public interface RefreshTokenRepository {

	void save(RefreshToken token);

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Revokes the token only if it is still active; returns whether this call revoked it. */
	boolean revoke(String tokenHash, Instant at);

	/** Revokes every still-active token of the user (e.g. when the account is deactivated); returns how many. */
	int revokeAllForUser(long userId, Instant at);

}
