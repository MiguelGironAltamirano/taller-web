package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.auth.application.PasskeyRepository;
import pe.edu.unmsm.servicios.auth.domain.Passkey;
import pe.edu.unmsm.servicios.auth.domain.PasskeyAlreadyRegisteredException;

@Repository
class JdbcPasskeyRepository implements PasskeyRepository {

	private static final String COLUMNS = "select id, user_id, credential_id, public_key, sign_count, transports, "
			+ "backup_eligible, backed_up, name, created_at, last_used_at from passkeys";

	private final JdbcTemplate jdbc;

	JdbcPasskeyRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Passkey save(Passkey passkey) {
		GeneratedKeyHolder key = new GeneratedKeyHolder();
		try {
			jdbc.update(connection -> {
				var statement = connection.prepareStatement("insert into passkeys (user_id, credential_id, public_key, "
						+ "sign_count, transports, backup_eligible, backed_up, name, created_at) "
						+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				statement.setLong(1, passkey.userId());
				statement.setBytes(2, passkey.credentialId());
				statement.setBytes(3, passkey.publicKey());
				statement.setLong(4, passkey.signCount());
				statement.setString(5, passkey.transports());
				statement.setBoolean(6, passkey.backupEligible());
				statement.setBoolean(7, passkey.backedUp());
				statement.setString(8, passkey.name());
				statement.setObject(9, utc(passkey.createdAt()));
				return statement;
			}, key);
		}
		catch (DuplicateKeyException e) {
			throw new PasskeyAlreadyRegisteredException();
		}
		return new Passkey(key.getKey().longValue(), passkey.userId(), passkey.credentialId(), passkey.publicKey(),
				passkey.signCount(), passkey.transports(), passkey.backupEligible(), passkey.backedUp(), passkey.name(),
				passkey.createdAt(), passkey.lastUsedAt());
	}

	@Override
	public Optional<Passkey> findByCredentialId(byte[] credentialId) {
		return jdbc.query(COLUMNS + " where credential_id = ?", JdbcPasskeyRepository::passkey, credentialId).stream()
				.findFirst();
	}

	@Override
	public List<Passkey> findByUser(long userId) {
		return jdbc.query(COLUMNS + " where user_id = ? order by created_at desc, id desc",
				JdbcPasskeyRepository::passkey, userId);
	}

	@Override
	public int countByUser(long userId) {
		Integer count = jdbc.queryForObject("select count(*) from passkeys where user_id = ?", Integer.class, userId);
		return count == null ? 0 : count;
	}

	@Override
	public Optional<Passkey> rename(long id, long userId, String name) {
		if (jdbc.update("update passkeys set name = ? where id = ? and user_id = ?", name, id, userId) == 0) {
			return Optional.empty();
		}
		return jdbc.query(COLUMNS + " where id = ?", JdbcPasskeyRepository::passkey, id).stream().findFirst();
	}

	@Override
	public boolean delete(long id, long userId) {
		return jdbc.update("delete from passkeys where id = ? and user_id = ?", id, userId) > 0;
	}

	@Override
	public void recordUse(long id, long signCount, boolean backedUp, Instant usedAt) {
		jdbc.update("update passkeys set sign_count = ?, backed_up = ?, last_used_at = ? where id = ?", signCount,
				backedUp, utc(usedAt), id);
	}

	private static Passkey passkey(ResultSet rs, int row) throws SQLException {
		LocalDateTime lastUsed = rs.getObject("last_used_at", LocalDateTime.class);
		return new Passkey(rs.getLong("id"), rs.getLong("user_id"), rs.getBytes("credential_id"),
				rs.getBytes("public_key"), rs.getLong("sign_count"), rs.getString("transports"),
				rs.getBoolean("backup_eligible"), rs.getBoolean("backed_up"), rs.getString("name"),
				rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
				lastUsed == null ? null : lastUsed.toInstant(ZoneOffset.UTC));
	}

	/** Timestamps are stored as UTC wall-clock values, like the rest of the schema. */
	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
