package pe.edu.unmsm.servicios.admin.domain;

public class ManagedUserNotFoundException extends RuntimeException {

	public ManagedUserNotFoundException(long id) {
		super("User not found: " + id);
	}

}
