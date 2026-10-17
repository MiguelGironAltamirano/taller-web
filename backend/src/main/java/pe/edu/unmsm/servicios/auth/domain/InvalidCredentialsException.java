package pe.edu.unmsm.servicios.auth.domain;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid credentials");
	}

}
