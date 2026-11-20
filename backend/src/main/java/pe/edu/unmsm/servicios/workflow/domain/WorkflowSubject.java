package pe.edu.unmsm.servicios.workflow.domain;

import java.time.Instant;

import pe.edu.unmsm.servicios.requests.domain.Cancellation;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * The part of a request the state machine looks at. {@code cancellation} and {@code now} only matter for a cancelled
 * request: where restoring brings it back and whether it is still in time.
 */
public record WorkflowSubject(RequestStatus status, long requesterId, long areaId, Long technicianId,
		Cancellation cancellation, Instant now) {

	public WorkflowSubject(RequestStatus status, long requesterId, long areaId, Long technicianId) {
		this(status, requesterId, areaId, technicianId, null, null);
	}

}
