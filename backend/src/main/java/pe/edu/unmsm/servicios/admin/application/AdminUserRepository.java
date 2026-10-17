package pe.edu.unmsm.servicios.admin.application;

import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.admin.domain.ManagedUser;

public interface AdminUserRepository {

	List<ManagedUser> findAllOrderedByName();

	Optional<ManagedUser> findById(long id);

	/** Persists a new active account; throws EmailAlreadyRegisteredException when the email is taken. */
	ManagedUser create(UserData data, String passwordHash);

	/** Updates an existing account; throws EmailAlreadyRegisteredException when the email belongs to another user. */
	ManagedUser update(long id, UserData data);

	ManagedUser setActive(long id, boolean active);

}
