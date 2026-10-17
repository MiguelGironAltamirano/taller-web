package pe.edu.unmsm.servicios.auth.domain;

public class EmailAlreadyRegisteredException extends RuntimeException {

	public EmailAlreadyRegisteredException(String email) {
		super("Email already registered: " + email);
	}

}
