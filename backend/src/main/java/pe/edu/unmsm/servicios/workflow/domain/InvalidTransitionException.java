package pe.edu.unmsm.servicios.workflow.domain;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** The transition is not in the table of the spec (409). */
public class InvalidTransitionException extends RuntimeException {

	private final RequestStatus from;
	private final RequestStatus to;

	public InvalidTransitionException(RequestStatus from, RequestStatus to) {
		super("Transition not allowed: " + from + " -> " + to);
		this.from = from;
		this.to = to;
	}

	public RequestStatus from() {
		return from;
	}

	public RequestStatus to() {
		return to;
	}

}
