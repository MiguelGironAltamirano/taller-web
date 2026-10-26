package pe.edu.unmsm.servicios.satisfaction.domain;

import java.util.Map;

/** The score or the comment is not valid (400); {@code errors} maps each field to its message. */
public class InvalidRatingException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidRatingException(Map<String, String> errors) {
		super("Invalid rating: " + errors);
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
