package pe.edu.unmsm.servicios.workflow.domain;

import java.util.Map;

/** The transition is allowed but a required piece of data is missing or wrong (400); {@code errors} maps field to message. */
public class InvalidTransitionDataException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidTransitionDataException(Map<String, String> errors) {
		super("Invalid transition data: " + errors);
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
