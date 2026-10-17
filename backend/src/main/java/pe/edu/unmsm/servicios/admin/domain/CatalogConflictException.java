package pe.edu.unmsm.servicios.admin.domain;

import java.util.Map;

/** The catalog change clashes with the current data (duplicate name, entry still in use). */
public class CatalogConflictException extends RuntimeException {

	private final Map<String, String> errors;

	public CatalogConflictException(String message) {
		this(message, Map.of());
	}

	public CatalogConflictException(String message, Map<String, String> errors) {
		super(message);
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
