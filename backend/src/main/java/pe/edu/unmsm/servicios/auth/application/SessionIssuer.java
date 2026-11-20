package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.auth.domain.User;

/**
 * Opens a session for a user already verified (by password or passkey): access token plus refresh token. A user has
 * at most one open session, so opening one closes the others (docs/api/contrato-v1.md «Una sola sesión por usuario»).
 */
public class SessionIssuer {

	private final RefreshTokenRepository refreshTokens;
	private final RefreshTokenCodec codec;
	private final AccessTokenGenerator accessTokens;
	private final Clock clock;
	private final Duration refreshTtl;

	public SessionIssuer(RefreshTokenRepository refreshTokens, RefreshTokenCodec codec,
			AccessTokenGenerator accessTokens, Clock clock, Duration refreshTtl) {
		this.refreshTokens = refreshTokens;
		this.codec = codec;
		this.accessTokens = accessTokens;
		this.clock = clock;
		this.refreshTtl = refreshTtl;
	}

	@Transactional
	public AuthSession issue(User user) {
		Instant now = clock.instant();
		refreshTokens.lockSessionsOf(user.id());
		refreshTokens.revokeAllForUser(user.id(), now);
		String sessionId = UUID.randomUUID().toString();
		String refreshToken = codec.generate();
		refreshTokens.save(new RefreshToken(null, user.id(), sessionId, codec.hash(refreshToken), now.plus(refreshTtl),
				null));
		return new AuthSession(user, accessTokens.generate(user, sessionId), refreshToken);
	}

}
