package pe.edu.unmsm.servicios.auth.application;

import java.time.Instant;
import java.util.Optional;

import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.shared.application.SessionRegistry.SessionState;

public interface RefreshTokenRepository {

	void save(RefreshToken token);

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Revokes the token only if it is still active; returns whether this call revoked it. */
	boolean revoke(String tokenHash, Instant at);

	/** Revokes every still-active token of the user (e.g. when the account is deactivated); returns how many. */
	int revokeAllForUser(long userId, Instant at);

	/** Revokes every still-active token of the user outside {@code keptSessionId}; returns how many. */
	int revokeAllForUserExcept(long userId, String keptSessionId, Instant at);

	/**
	 * Locks the user's sessions until the current transaction ends, so two logins of the same user run one after
	 * the other and only one session stays open.
	 */
	void lockSessionsOf(long userId);

	SessionState stateOf(long userId, String sessionId, Instant now);

}
