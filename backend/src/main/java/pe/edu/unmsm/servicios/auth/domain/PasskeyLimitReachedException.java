package pe.edu.unmsm.servicios.auth.domain;

public class PasskeyLimitReachedException extends RuntimeException {

	private final int max;

	public PasskeyLimitReachedException(int max) {
		super("The user already has " + max + " passkeys");
		this.max = max;
	}

	public int max() {
		return max;
	}

}
