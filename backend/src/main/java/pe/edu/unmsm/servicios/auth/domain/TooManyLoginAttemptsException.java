package pe.edu.unmsm.servicios.auth.domain;

public class TooManyLoginAttemptsException extends RuntimeException {

	private final long retryAfterSeconds;

	public TooManyLoginAttemptsException(long retryAfterSeconds) {
		super("Too many failed login attempts");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}

}
