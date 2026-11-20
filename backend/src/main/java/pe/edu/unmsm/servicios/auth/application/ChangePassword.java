package pe.edu.unmsm.servicios.auth.application;

import java.time.Instant;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.domain.InvalidCurrentPasswordException;
import pe.edu.unmsm.servicios.auth.domain.User;

public class ChangePassword {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordHasher passwordHasher;

	public ChangePassword(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			PasswordHasher passwordHasher) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordHasher = passwordHasher;
	}

	/** Changes the password and closes every session of the user except {@code currentSessionId}, the one asking. */
	// Sessions of a user change one at a time (lockSessionsOf); READ_COMMITTED lets the read after that lock see what
	// the previous login or refresh committed, and keeps MySQL from locking index gaps (see JpaRefreshTokenRepository).
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void execute(long userId, String currentSessionId, String currentPassword, String newPassword) {
		User user = userRepository.findById(userId)
				.orElseThrow(InvalidCurrentPasswordException::new);

		if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
			throw new InvalidCurrentPasswordException();
		}

		String newHash = passwordHasher.hash(newPassword);
		userRepository.updatePassword(userId, newHash);
		refreshTokenRepository.lockSessionsOf(userId);
		refreshTokenRepository.revokeAllForUserExcept(userId, currentSessionId, Instant.now());
	}

}
