package pe.edu.unmsm.servicios.requests.domain;

/** The request is in a status that no longer accepts the change (409). */
public class RequestNotModifiableException extends RuntimeException {

	public RequestNotModifiableException(String message) {
		super(message);
	}

}
