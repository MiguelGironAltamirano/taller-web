package pe.edu.unmsm.servicios.auth.domain;

public class PasskeyAlreadyRegisteredException extends RuntimeException {

	public PasskeyAlreadyRegisteredException() {
		super("The credential is already registered");
	}

}
