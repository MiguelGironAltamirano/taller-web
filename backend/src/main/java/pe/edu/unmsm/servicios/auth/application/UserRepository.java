package pe.edu.unmsm.servicios.auth.application;

import java.util.Optional;

import pe.edu.unmsm.servicios.auth.domain.User;

public interface UserRepository {

	Optional<User> findByEmail(String email);

	Optional<User> findById(long id);

	/** Persists a new user; throws EmailAlreadyRegisteredException when the email is taken. */
	User register(User user);

}
