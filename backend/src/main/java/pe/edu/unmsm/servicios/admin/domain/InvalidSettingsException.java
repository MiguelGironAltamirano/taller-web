package pe.edu.unmsm.servicios.admin.domain;

import java.util.Map;

public class InvalidSettingsException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidSettingsException(Map<String, String> errors) {
		super("Invalid system settings: " + errors.keySet());
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
