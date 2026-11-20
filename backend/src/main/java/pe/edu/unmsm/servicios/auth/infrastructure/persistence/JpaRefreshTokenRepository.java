package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;
import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.shared.application.SessionRegistry.SessionState;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

	private final SpringDataRefreshTokenRepository repository;
	JpaRefreshTokenRepository(SpringDataRefreshTokenRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(RefreshToken token) {
		RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.userId = token.userId();
		entity.sessionId = token.sessionId();
		entity.tokenHash = token.tokenHash();
		entity.expiresAt = token.expiresAt();
		entity.revokedAt = token.revokedAt();
		entity.createdAt = Instant.now();
		repository.save(entity);
	}

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return repository.findByTokenHash(tokenHash).map(entity -> new RefreshToken(entity.id, entity.userId,
				entity.sessionId, entity.tokenHash, entity.expiresAt, entity.revokedAt));
	}

	@Override
	public boolean revoke(String tokenHash, Instant at) {
		return repository.revokeIfActive(tokenHash, at) > 0;
	}

	/**
	 * Revokes by primary key, never with {@code where user_id = ?}: in MySQL that update locks the gap of the
	 * user_id index when the user has no active token (a first login), users with neighbouring ids share that gap,
	 * and their inserts of the new token then deadlocked (found by the load test, infra/k6). Reading the ids is a
	 * plain non-locking read; logins and refreshes of one user are serialized by {@link #lockSessionsOf}, so no
	 * token of that user can appear between the read and the update.
	 */
	@Override
	public int revokeAllForUser(long userId, Instant at) {
		return revokeByIds(repository.findActiveIdsOfUser(userId), at);
	}

	@Override
	public int revokeAllForUserExcept(long userId, String keptSessionId, Instant at) {
		return revokeByIds(repository.findActiveIdsOfUserExcept(userId, keptSessionId), at);
	}

	private int revokeByIds(java.util.List<Long> ids, Instant at) {
		return ids.isEmpty() ? 0 : repository.revokeByIds(ids, at);
	}

	@Override
	public void lockSessionsOf(long userId) {
		repository.lockUser(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public SessionState stateOf(long userId, String sessionId, Instant now) {
		if (repository.existsBySessionIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(sessionId, userId, now)) {
			return SessionState.OPEN;
		}
		return repository.existsByUserIdAndSessionIdNotAndRevokedAtIsNullAndExpiresAtAfter(userId, sessionId, now)
				? SessionState.REPLACED : SessionState.CLOSED;
	}

}
