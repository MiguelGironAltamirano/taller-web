package pe.edu.unmsm.servicios.requests.domain;

/** The user can see the request but may not make this change to it (403). */
public class RequestChangeForbiddenException extends RuntimeException {

	public RequestChangeForbiddenException(String message) {
		super(message);
	}

}
