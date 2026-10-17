package pe.edu.unmsm.servicios.auth.domain;

public class InvalidCurrentPasswordException extends RuntimeException {

	public InvalidCurrentPasswordException() {
		super("La contraseña actual no es correcta");
	}

}
