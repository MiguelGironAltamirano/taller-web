package pe.edu.unmsm.servicios.workflow.domain;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * What a valid transition changes: the new status, the technician and closure reason the request ends with,
 * the text and technician recorded in the history, and the solution proposed when the step resolves the request.
 */
public record TransitionOutcome(RequestStatus to, Long technicianId, ClosureReason closureReason, String reason,
		Long historyTechnicianId, String solution) {

	/** An outcome that proposes no solution. */
	public TransitionOutcome(RequestStatus to, Long technicianId, ClosureReason closureReason, String reason,
			Long historyTechnicianId) {
		this(to, technicianId, closureReason, reason, historyTechnicianId, null);
	}

}
