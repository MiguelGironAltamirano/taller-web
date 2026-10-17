package pe.edu.unmsm.servicios.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.auth.application.AccessTokenGenerator;
import pe.edu.unmsm.servicios.auth.application.PasswordHasher;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenCodec;
import pe.edu.unmsm.servicios.auth.domain.User;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.AccessTokenIssuer;
import pe.edu.unmsm.servicios.shared.infrastructure.security.AccessTokenIssuer.IssuedToken;

final class AuthAdapters {

	private AuthAdapters() {
	}

	@Component
	static class BcryptPasswordHasher implements PasswordHasher {

		private final PasswordEncoder encoder;

		BcryptPasswordHasher(PasswordEncoder encoder) {
			this.encoder = encoder;
		}

		@Override
		public String hash(String rawPassword) {
			return encoder.encode(rawPassword);
		}

		@Override
		public boolean matches(String rawPassword, String hash) {
			return encoder.matches(rawPassword, hash);
		}

	}

	@Component
	static class SecureRefreshTokenCodec implements RefreshTokenCodec {

		private static final int TOKEN_BYTES = 32;

		private final SecureRandom random = new SecureRandom();

		@Override
		public String generate() {
			byte[] bytes = new byte[TOKEN_BYTES];
			random.nextBytes(bytes);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		}

		@Override
		public String hash(String token) {
			try {
				return HexFormat.of().formatHex(
						MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException("SHA-256 is not available", ex);
			}
		}

	}

	@Component
	static class JwtAccessTokenGenerator implements AccessTokenGenerator {

		private final AccessTokenIssuer issuer;

		JwtAccessTokenGenerator(AccessTokenIssuer issuer) {
			this.issuer = issuer;
		}

		@Override
		public AccessToken generate(User user) {
			IssuedToken issued = issuer.issue(
					new AuthenticatedUser(user.id(), user.email(), user.fullName(), user.role().name()));
			return new AccessToken(issued.value(), issued.expiresInSeconds());
		}

	}

}
