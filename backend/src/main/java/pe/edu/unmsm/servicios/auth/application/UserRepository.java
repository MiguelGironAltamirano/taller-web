package pe.edu.unmsm.servicios.auth.application;

import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.auth.domain.Role;
import pe.edu.unmsm.servicios.auth.domain.User;

public interface UserRepository {

	Optional<User> findByEmail(String email);

	Optional<User> findById(long id);

	/** Active users of the given role assigned to the area, ordered by name. */
	List<User> findActiveByRoleAndArea(Role role, long areaId);

	/** Persists a new user; throws EmailAlreadyRegisteredException when the email is taken. */
	User register(User user);

	/** Updates the password hash of the user with the given id. */
	void updatePassword(long id, String newPasswordHash);

}
