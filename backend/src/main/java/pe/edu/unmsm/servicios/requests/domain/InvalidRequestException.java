package pe.edu.unmsm.servicios.requests.domain;

import java.util.Map;

/** The submitted data breaks a business rule; {@code errors} maps each field to its message. */
public class InvalidRequestException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidRequestException(Map<String, String> errors) {
		super("Invalid request data: " + errors);
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
