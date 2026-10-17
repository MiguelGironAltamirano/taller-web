package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;
import pe.edu.unmsm.servicios.auth.domain.RefreshToken;

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
		entity.tokenHash = token.tokenHash();
		entity.expiresAt = token.expiresAt();
		entity.revokedAt = token.revokedAt();
		entity.createdAt = Instant.now();
		repository.save(entity);
	}

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return repository.findByTokenHash(tokenHash).map(entity -> new RefreshToken(entity.id, entity.userId,
				entity.tokenHash, entity.expiresAt, entity.revokedAt));
	}

	@Override
	public boolean revoke(String tokenHash, Instant at) {
		return repository.revokeIfActive(tokenHash, at) > 0;
	}

	@Override
	public int revokeAllForUser(long userId, Instant at) {
		return repository.revokeAllActiveOfUser(userId, at);
	}

}
