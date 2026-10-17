package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.domain.InvalidRefreshTokenException;
import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.auth.domain.User;

public class RefreshSession {

	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final RefreshTokenCodec codec;
	private final AccessTokenGenerator accessTokens;
	private final Clock clock;
	private final Duration refreshTtl;

	public RefreshSession(UserRepository users, RefreshTokenRepository refreshTokens, RefreshTokenCodec codec,
			AccessTokenGenerator accessTokens, Clock clock, Duration refreshTtl) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.codec = codec;
		this.accessTokens = accessTokens;
		this.clock = clock;
		this.refreshTtl = refreshTtl;
	}

	@Transactional
	public AuthSession execute(String presentedToken) {
		if (presentedToken == null || presentedToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		Instant now = clock.instant();
		String hash = codec.hash(presentedToken);
		RefreshToken current = refreshTokens.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);
		if (!current.isUsableAt(now) || !refreshTokens.revoke(hash, now)) {
			throw new InvalidRefreshTokenException();
		}
		User user = users.findById(current.userId()).filter(User::active).orElseThrow(InvalidRefreshTokenException::new);
		String next = codec.generate();
		refreshTokens.save(new RefreshToken(null, user.id(), codec.hash(next), now.plus(refreshTtl), null));
		return new AuthSession(user, accessTokens.generate(user), next);
	}

}
