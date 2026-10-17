package pe.edu.unmsm.servicios.auth.application;

import java.time.Instant;

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

	@Transactional
	public void execute(long userId, String currentPassword, String newPassword) {
		User user = userRepository.findById(userId)
				.orElseThrow(InvalidCurrentPasswordException::new);

		if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
			throw new InvalidCurrentPasswordException();
		}

		String newHash = passwordHasher.hash(newPassword);
		userRepository.updatePassword(userId, newHash);
		refreshTokenRepository.revokeAllForUser(userId, Instant.now());
	}

}
