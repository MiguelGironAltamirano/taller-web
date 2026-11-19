package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.auth.application.WebAuthnUserHandles;

@Repository
class JdbcWebAuthnUserHandles implements WebAuthnUserHandles {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final JdbcTemplate jdbc;

	JdbcWebAuthnUserHandles(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<byte[]> find(long userId) {
		return jdbc.query("select webauthn_user_handle from users where id = ?",
				(rs, row) -> rs.getBytes("webauthn_user_handle"), userId).stream()
				.filter(handle -> handle != null)
				.findFirst();
	}

	/** Only sets the handle while it is still null, so two concurrent registrations end up with the same one. */
	@Override
	public byte[] getOrCreate(long userId) {
		byte[] handle = new byte[64];
		RANDOM.nextBytes(handle);
		jdbc.update("update users set webauthn_user_handle = ? where id = ? and webauthn_user_handle is null", handle,
				userId);
		return find(userId).orElseThrow();
	}

}
