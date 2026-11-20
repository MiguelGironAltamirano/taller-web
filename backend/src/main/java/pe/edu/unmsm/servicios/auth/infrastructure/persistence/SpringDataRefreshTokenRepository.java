package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	@Modifying
	@Query("update RefreshTokenEntity t set t.revokedAt = :at where t.tokenHash = :hash and t.revokedAt is null")
	int revokeIfActive(@Param("hash") String hash, @Param("at") Instant at);

	@Modifying
	@Query("update RefreshTokenEntity t set t.revokedAt = :at where t.userId = :userId and t.revokedAt is null")
	int revokeAllActiveOfUser(@Param("userId") long userId, @Param("at") Instant at);

	@Modifying
	@Query("update RefreshTokenEntity t set t.revokedAt = :at where t.userId = :userId and t.revokedAt is null "
			+ "and t.sessionId <> :kept")
	int revokeAllActiveOfUserExcept(@Param("userId") long userId, @Param("kept") String keptSessionId,
			@Param("at") Instant at);

	/** The user row is the lock: every login of the user takes it before closing and opening sessions. */
	@Query(value = "select id from users where id = :userId for update", nativeQuery = true)
	Optional<Long> lockUser(@Param("userId") long userId);

	boolean existsBySessionIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(String sessionId, long userId, Instant now);

	boolean existsByUserIdAndSessionIdNotAndRevokedAtIsNullAndExpiresAtAfter(long userId, String sessionId, Instant now);

}
