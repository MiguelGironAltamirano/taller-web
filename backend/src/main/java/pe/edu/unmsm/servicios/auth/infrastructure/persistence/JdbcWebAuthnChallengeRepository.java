package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.auth.application.WebAuthnChallengeRepository;
import pe.edu.unmsm.servicios.auth.domain.ChallengePurpose;

@Repository
class JdbcWebAuthnChallengeRepository implements WebAuthnChallengeRepository {

	private final JdbcTemplate jdbc;

	JdbcWebAuthnChallengeRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void save(String id, ChallengePurpose purpose, Long userId, byte[] challenge, Instant expiresAt) {
		jdbc.update("insert into webauthn_challenges (id, purpose, user_id, challenge, expires_at) values (?, ?, ?, ?, ?)",
				id, purpose.name(), userId, challenge, utc(expiresAt));
	}

	/** The conditional update is the single-use guarantee: two concurrent consumers cannot both update the row. */
	@Override
	public Optional<byte[]> consume(String id, ChallengePurpose purpose, Long userId, Instant now) {
		if (id == null) {
			return Optional.empty();
		}
		int updated = jdbc.update("update webauthn_challenges set used_at = ? where id = ? and purpose = ? "
				+ "and user_id <=> ? and used_at is null and expires_at > ?", utc(now), id, purpose.name(), userId,
				utc(now));
		if (updated == 0) {
			return Optional.empty();
		}
		return Optional.ofNullable(
				jdbc.queryForObject("select challenge from webauthn_challenges where id = ?", byte[].class, id));
	}

	@Override
	public void deleteExpired(Instant now) {
		jdbc.update("delete from webauthn_challenges where expires_at <= ?", utc(now));
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
