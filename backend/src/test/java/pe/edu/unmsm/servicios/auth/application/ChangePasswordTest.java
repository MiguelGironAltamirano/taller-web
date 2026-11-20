package pe.edu.unmsm.servicios.auth.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.auth.domain.InvalidCurrentPasswordException;
import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;
import pe.edu.unmsm.servicios.auth.domain.User;
import pe.edu.unmsm.servicios.shared.application.SessionRegistry.SessionState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangePasswordTest {

	private InMemoryUserRepository userRepository;
	private InMemoryRefreshTokenRepository refreshTokenRepository;
	private FakePasswordHasher passwordHasher;
	private ChangePassword changePassword;

	@BeforeEach
	void setUp() {
		userRepository = new InMemoryUserRepository();
		refreshTokenRepository = new InMemoryRefreshTokenRepository();
		passwordHasher = new FakePasswordHasher();
		changePassword = new ChangePassword(userRepository, refreshTokenRepository, passwordHasher);

		User user = new User(1L, "Angel Carrasco", "acarrasco@unmsm.edu.pe", "hashed_old_pass",
				Role.SOLICITANTE, RequesterType.ESTUDIANTE, null, true);
		userRepository.users.add(user);
	}

	@Test
	void wrongCurrentPasswordThrowsInvalidCurrentPasswordException() {
		assertThatThrownBy(() -> changePassword.execute(1L, "current-session", "wrong_pass", "new_secret_123"))
				.isInstanceOf(InvalidCurrentPasswordException.class);

		assertThat(userRepository.users.get(0).passwordHash()).isEqualTo("hashed_old_pass");
		assertThat(refreshTokenRepository.revokedUsers).isEmpty();
	}

	@Test
	void correctPasswordUpdatesPasswordHashAndClosesTheOtherSessions() {
		changePassword.execute(1L, "current-session", "old_pass", "new_secret_123");

		assertThat(userRepository.users.get(0).passwordHash()).isEqualTo("hashed_new_secret_123");
		assertThat(refreshTokenRepository.revokedUsers).containsExactly(1L);
		assertThat(refreshTokenRepository.keptSessions).containsExactly("current-session");
	}

	private static class InMemoryUserRepository implements UserRepository {
		final List<User> users = new ArrayList<>();

		@Override
		public Optional<User> findByEmail(String email) {
			return users.stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst();
		}

		@Override
		public Optional<User> findById(long id) {
			return users.stream().filter(u -> u.id() != null && u.id() == id).findFirst();
		}

		@Override
		public List<User> findActiveByRoleAndArea(Role role, long areaId) {
			return List.of();
		}

		@Override
		public User register(User user) {
			users.add(user);
			return user;
		}

		@Override
		public void updatePassword(long id, String newPasswordHash) {
			for (int i = 0; i < users.size(); i++) {
				User u = users.get(i);
				if (u.id() != null && u.id() == id) {
					users.set(i, new User(u.id(), u.fullName(), u.email(), newPasswordHash, u.role(),
							u.requesterType(), u.areaId(), u.active()));
					return;
				}
			}
		}
	}

	private static class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
		final List<Long> revokedUsers = new ArrayList<>();
		final List<String> keptSessions = new ArrayList<>();

		@Override
		public void save(RefreshToken token) {}

		@Override
		public Optional<RefreshToken> findByTokenHash(String tokenHash) {
			return Optional.empty();
		}

		@Override
		public boolean revoke(String tokenHash, Instant at) {
			return false;
		}

		@Override
		public int revokeAllForUser(long userId, Instant at) {
			throw new AssertionError("changing the password keeps the current session");
		}

		@Override
		public int revokeAllForUserExcept(long userId, String keptSessionId, Instant at) {
			revokedUsers.add(userId);
			keptSessions.add(keptSessionId);
			return 1;
		}

		@Override
		public void lockSessionsOf(long userId) {}

		@Override
		public SessionState stateOf(long userId, String sessionId, Instant now) {
			return SessionState.CLOSED;
		}
	}

	private static class FakePasswordHasher implements PasswordHasher {
		@Override
		public String hash(String raw) {
			return "hashed_" + raw;
		}

		@Override
		public boolean matches(String raw, String hashed) {
			return ("hashed_" + raw).equals(hashed);
		}
	}
}
