package pe.edu.unmsm.servicios.auth.domain;

/** A WebAuthn response that cannot be parsed or verified, or a challenge that is unknown, expired or already used. */
public class InvalidPasskeyException extends RuntimeException {

	public InvalidPasskeyException(String message) {
		super(message);
	}

	public InvalidPasskeyException(String message, Throwable cause) {
		super(message, cause);
	}

}
