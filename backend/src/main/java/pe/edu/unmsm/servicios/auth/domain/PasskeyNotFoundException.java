package pe.edu.unmsm.servicios.auth.domain;

/** The passkey does not exist or belongs to another user; both cases look the same to the caller. */
public class PasskeyNotFoundException extends RuntimeException {

	public PasskeyNotFoundException(long id) {
		super("Passkey not found: " + id);
	}

}
