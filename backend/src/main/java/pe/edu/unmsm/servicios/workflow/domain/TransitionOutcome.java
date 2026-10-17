package pe.edu.unmsm.servicios.workflow.domain;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * What a valid transition changes: the new status, the technician and closure reason the request ends with,
 * and the text and technician recorded in the history.
 */
public record TransitionOutcome(RequestStatus to, Long technicianId, ClosureReason closureReason, String reason,
		Long historyTechnicianId) {
}
