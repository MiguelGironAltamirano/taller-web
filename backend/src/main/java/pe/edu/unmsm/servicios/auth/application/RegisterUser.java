package pe.edu.unmsm.servicios.auth.application;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;
import pe.edu.unmsm.servicios.auth.domain.User;

public class RegisterUser {

	public record Command(String fullName, String email, String password, RequesterType requesterType) {
	}

	private final UserRepository users;
	private final PasswordHasher hasher;

	public RegisterUser(UserRepository users, PasswordHasher hasher) {
		this.users = users;
		this.hasher = hasher;
	}

	@Transactional
	public User execute(Command command) {
		String email = command.email().trim().toLowerCase(Locale.ROOT);
		User user = new User(null, command.fullName().trim(), email, hasher.hash(command.password()), Role.SOLICITANTE,
				command.requesterType(), null, true);
		return users.register(user);
	}

}
