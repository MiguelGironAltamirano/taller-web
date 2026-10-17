package pe.edu.unmsm.servicios.workflow.domain;

/** The transition exists but this user may not perform it (403). */
public class TransitionForbiddenException extends RuntimeException {

	public TransitionForbiddenException(String message) {
		super(message);
	}

}
