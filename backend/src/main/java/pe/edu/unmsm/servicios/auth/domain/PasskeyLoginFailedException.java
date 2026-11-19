package pe.edu.unmsm.servicios.auth.domain;

/** Any failure of a passkey login; deliberately says nothing about which check failed. */
public class PasskeyLoginFailedException extends RuntimeException {

	public PasskeyLoginFailedException() {
		super("Passkey login failed");
	}

}
