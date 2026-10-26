package pe.edu.unmsm.servicios.dashboard.domain;

import java.util.Map;

/** A dashboard query parameter is not valid; {@link #errors()} maps each parameter to its message. */
public class InvalidDashboardQueryException extends RuntimeException {

	private final transient Map<String, String> errors;

	public InvalidDashboardQueryException(String parameter, String message) {
		super(message);
		this.errors = Map.of(parameter, message);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
