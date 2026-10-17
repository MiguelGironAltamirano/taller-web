package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "user_id", nullable = false)
	long userId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
	String tokenHash;

	@Column(name = "expires_at", nullable = false)
	Instant expiresAt;

	@Column(name = "revoked_at")
	Instant revokedAt;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

}
