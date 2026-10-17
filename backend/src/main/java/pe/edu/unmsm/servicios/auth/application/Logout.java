package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

public class Logout {

	private final RefreshTokenRepository refreshTokens;
	private final RefreshTokenCodec codec;
	private final Clock clock;

	public Logout(RefreshTokenRepository refreshTokens, RefreshTokenCodec codec, Clock clock) {
		this.refreshTokens = refreshTokens;
		this.codec = codec;
		this.clock = clock;
	}

	@Transactional
	public void execute(String presentedToken) {
		if (presentedToken != null && !presentedToken.isBlank()) {
			refreshTokens.revoke(codec.hash(presentedToken), clock.instant());
		}
	}

}
