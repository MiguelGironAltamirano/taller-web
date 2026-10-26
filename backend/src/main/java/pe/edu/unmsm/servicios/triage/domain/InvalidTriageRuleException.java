package pe.edu.unmsm.servicios.triage.domain;

import java.util.Map;

/** The rule body breaks a validation rule; {@code errors} maps each field to its message. */
public class InvalidTriageRuleException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidTriageRuleException(Map<String, String> errors) {
		super("Invalid triage rule: " + errors.keySet());
		this.errors = Map.copyOf(errors);
	}

	public Map<String, String> errors() {
		return errors;
	}

}
