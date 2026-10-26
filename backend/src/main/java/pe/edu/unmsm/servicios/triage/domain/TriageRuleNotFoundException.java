package pe.edu.unmsm.servicios.triage.domain;

public class TriageRuleNotFoundException extends RuntimeException {

	public TriageRuleNotFoundException() {
		super("La regla de triage no existe");
	}

}
