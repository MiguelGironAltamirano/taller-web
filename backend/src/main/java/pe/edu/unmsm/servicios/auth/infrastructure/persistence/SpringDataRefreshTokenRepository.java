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

}
