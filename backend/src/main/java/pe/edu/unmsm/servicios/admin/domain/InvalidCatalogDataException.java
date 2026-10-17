package pe.edu.unmsm.servicios.admin.domain;

import java.util.Map;

public class InvalidCatalogDataException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidCatalogDataException(Map<String, String> errors) {
		super("Invalid catalog data: " + errors.keySet());
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
