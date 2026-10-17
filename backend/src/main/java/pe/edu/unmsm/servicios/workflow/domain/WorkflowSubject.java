package pe.edu.unmsm.servicios.workflow.domain;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** The part of a request the state machine looks at. */
public record WorkflowSubject(RequestStatus status, long requesterId, long areaId, Long technicianId) {
}
