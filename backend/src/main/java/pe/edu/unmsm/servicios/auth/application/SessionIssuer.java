package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.auth.domain.User;

/** Opens a session for a user already verified (by password or passkey): access token plus refresh token. */
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

	public AuthSession issue(User user) {
		String refreshToken = codec.generate();
		Instant now = clock.instant();
		refreshTokens.save(new RefreshToken(null, user.id(), codec.hash(refreshToken), now.plus(refreshTtl), null));
		return new AuthSession(user, accessTokens.generate(user), refreshToken);
	}

}
