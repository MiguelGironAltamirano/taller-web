package pe.edu.unmsm.servicios.workflow.domain;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** What the user asks for: the target status plus the data some transitions need. */
public record TransitionCommand(RequestStatus to, String reason, Long technicianId, ClosureReason closureReason) {
}
