package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.domain.InvalidRefreshTokenException;
import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.auth.domain.SessionReplacedException;
import pe.edu.unmsm.servicios.auth.domain.User;
import pe.edu.unmsm.servicios.shared.application.SessionRegistry.SessionState;

/** Rotates the refresh token; the new one belongs to the same session, so the session id does not change. */
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

	/**
	 * @throws SessionReplacedException when the token's session was closed by a newer login of the same user
	 * @throws InvalidRefreshTokenException for any other unusable token
	 */
	// Sessions of a user change one at a time (lockSessionsOf); READ_COMMITTED lets the read after that lock see what
	// the previous login or refresh committed, and keeps MySQL from locking index gaps (see JpaRefreshTokenRepository).
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public AuthSession execute(String presentedToken) {
		if (presentedToken == null || presentedToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		Instant now = clock.instant();
		String hash = codec.hash(presentedToken);
		RefreshToken current = refreshTokens.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);
		// One login or refresh of a user at a time: a login closing the sessions never misses the token created here.
		refreshTokens.lockSessionsOf(current.userId());
		if (!current.isUsableAt(now) || !refreshTokens.revoke(hash, now)) {
			if (refreshTokens.stateOf(current.userId(), current.sessionId(), now) == SessionState.REPLACED) {
				throw new SessionReplacedException();
			}
			throw new InvalidRefreshTokenException();
		}
		User user = users.findById(current.userId()).filter(User::active).orElseThrow(InvalidRefreshTokenException::new);
		String next = codec.generate();
		refreshTokens.save(new RefreshToken(null, user.id(), current.sessionId(), codec.hash(next), now.plus(refreshTtl),
				null));
		return new AuthSession(user, accessTokens.generate(user, current.sessionId()), next);
	}

}
