package pe.edu.unmsm.servicios.admin.domain;

import java.util.Map;

public class InvalidUserDataException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidUserDataException(Map<String, String> errors) {
		super("Invalid user data: " + errors.keySet());
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
