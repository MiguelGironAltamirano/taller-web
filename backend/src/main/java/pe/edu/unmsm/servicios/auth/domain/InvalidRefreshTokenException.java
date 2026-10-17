package pe.edu.unmsm.servicios.auth.domain;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Invalid refresh token");
	}

}
