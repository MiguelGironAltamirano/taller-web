package pe.edu.unmsm.servicios.auth.domain;

/** The session was closed because the same user logged in somewhere else. */
public class SessionReplacedException extends RuntimeException {

	public SessionReplacedException() {
		super("The session was replaced by a newer login");
	}

}
