package pe.edu.unmsm.servicios.requests.domain;

/** The request does not exist or the user may not see it (both look the same, so its existence is not revealed). */
public class RequestNotFoundException extends RuntimeException {

	public RequestNotFoundException(String code) {
		super("Request not found: " + code);
	}

}
