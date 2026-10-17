package pe.edu.unmsm.servicios.admin.application;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.admin.domain.ManagedUser;
import pe.edu.unmsm.servicios.auth.application.PasswordHasher;

public class CreateManagedUser {

	/** The temporary password is returned only here; only its hash is stored. */
	public record Result(ManagedUser user, String temporaryPassword) {
	}

	private final AdminUserRepository users;
	private final AreaDirectory areas;
	private final PasswordHasher hasher;
	private final TemporaryPasswordGenerator passwords;

	public CreateManagedUser(AdminUserRepository users, AreaDirectory areas, PasswordHasher hasher,
			TemporaryPasswordGenerator passwords) {
		this.users = users;
		this.areas = areas;
		this.hasher = hasher;
		this.passwords = passwords;
	}

	@Transactional
	public Result execute(UserData data) {
		UserData valid = UserDataValidator.validate(data, areas);
		String temporaryPassword = passwords.generate();
		ManagedUser user = users.create(valid, hasher.hash(temporaryPassword));
		return new Result(user, temporaryPassword);
	}

}
