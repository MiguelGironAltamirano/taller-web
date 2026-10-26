package pe.edu.unmsm.servicios.workflow.application;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.event.RequestTransitionedEvent;

/** Builds the event published after every status change, whether a user or the system made it. */
final class TransitionEvents {

	private TransitionEvents() {
	}

	/** {@code actorId} is null when the system made the change. */
	static RequestTransitionedEvent of(ServiceRequest result, Long actorId) {
		return new RequestTransitionedEvent(result.code(), result.id(), result.requesterId(), result.areaId(),
				result.technicianId(), actorId, result.status().name(), result.status() == RequestStatus.ASIGNADA);
	}

}
