package pe.edu.unmsm.servicios.auth.application;

import java.util.Locale;
import java.util.Optional;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.domain.InvalidCredentialsException;
import pe.edu.unmsm.servicios.auth.domain.User;

public class Login {

	/** Valid BCrypt hash compared against when the email is unknown, so both failures cost the same. */
	private static final String DUMMY_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	private final UserRepository users;
	private final PasswordHasher hasher;
	private final SessionIssuer sessions;
	private final LoginAttemptLimiter limiter;

	public Login(UserRepository users, PasswordHasher hasher, SessionIssuer sessions, LoginAttemptLimiter limiter) {
		this.users = users;
		this.hasher = hasher;
		this.sessions = sessions;
		this.limiter = limiter;
	}

	// Sessions of a user change one at a time (lockSessionsOf); READ_COMMITTED lets the read after that lock see what
	// the previous login or refresh committed, and keeps MySQL from locking index gaps (see JpaRefreshTokenRepository).
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public AuthSession execute(String email, String password, String clientIp) {
		limiter.acquire(email, clientIp);
		Optional<User> found = users.findByEmail(email.trim().toLowerCase(Locale.ROOT));
		boolean passwordMatches = hasher.matches(password, found.map(User::passwordHash).orElse(DUMMY_HASH));
		if (found.isEmpty() || !passwordMatches || !found.get().active()) {
			throw new InvalidCredentialsException();
		}
		limiter.recordSuccess(email, clientIp);
		return sessions.issue(found.get());
	}

}
