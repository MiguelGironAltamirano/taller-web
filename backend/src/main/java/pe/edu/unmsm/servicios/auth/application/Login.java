package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.domain.InvalidCredentialsException;
import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.auth.domain.User;

public class Login {

	/** Valid BCrypt hash compared against when the email is unknown, so both failures cost the same. */
	private static final String DUMMY_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordHasher hasher;
	private final RefreshTokenCodec codec;
	private final AccessTokenGenerator accessTokens;
	private final Clock clock;
	private final Duration refreshTtl;
	private final LoginAttemptLimiter limiter;

	public Login(UserRepository users, RefreshTokenRepository refreshTokens, PasswordHasher hasher,
			RefreshTokenCodec codec, AccessTokenGenerator accessTokens, Clock clock, Duration refreshTtl,
			LoginAttemptLimiter limiter) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.hasher = hasher;
		this.codec = codec;
		this.accessTokens = accessTokens;
		this.clock = clock;
		this.refreshTtl = refreshTtl;
		this.limiter = limiter;
	}

	@Transactional
	public AuthSession execute(String email, String password) {
		limiter.checkAllowed(email);
		Optional<User> found = users.findByEmail(email.trim().toLowerCase(Locale.ROOT));
		boolean passwordMatches = hasher.matches(password, found.map(User::passwordHash).orElse(DUMMY_HASH));
		if (found.isEmpty() || !passwordMatches || !found.get().active()) {
			limiter.recordFailure(email);
			throw new InvalidCredentialsException();
		}
		limiter.recordSuccess(email);
		User user = found.get();
		String refreshToken = codec.generate();
		Instant now = clock.instant();
		refreshTokens.save(new RefreshToken(null, user.id(), codec.hash(refreshToken), now.plus(refreshTtl), null));
		return new AuthSession(user, accessTokens.generate(user), refreshToken);
	}

}
