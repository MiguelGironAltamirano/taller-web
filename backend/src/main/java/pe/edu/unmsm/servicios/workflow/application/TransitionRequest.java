package pe.edu.unmsm.servicios.workflow.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.Cancellation;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.domain.StateMachine;
import pe.edu.unmsm.servicios.workflow.domain.Technician;
import pe.edu.unmsm.servicios.workflow.domain.TransitionCommand;
import pe.edu.unmsm.servicios.workflow.domain.TransitionOutcome;
import pe.edu.unmsm.servicios.workflow.domain.WorkflowSubject;

import org.springframework.context.ApplicationEventPublisher;

public class TransitionRequest {

	private final RequestStateStore requests;
	private final TechnicianDirectory technicians;
	private final Clock clock;
	private final ApplicationEventPublisher eventPublisher;

	public TransitionRequest(RequestStateStore requests, TechnicianDirectory technicians, Clock clock) {
		this(requests, technicians, clock, null);
	}

	public TransitionRequest(RequestStateStore requests, TechnicianDirectory technicians, Clock clock,
			ApplicationEventPublisher eventPublisher) {
		this.requests = requests;
		this.technicians = technicians;
		this.clock = clock;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Applies a transition. The request is locked while it is read, so the decision is made on its current state
	 * and two simultaneous transitions are decided one after the other; the second one then sees the new state.
	 * If anything fails nothing is written.
	 */
	@Transactional
	public ServiceRequest execute(AuthenticatedUser actor, String code, TransitionCommand command) {
		ServiceRequest request = requests.findByCodeForUpdate(code).orElseThrow(() -> new RequestNotFoundException(code));
		Technician technician = command.technicianId() == null ? null
				: technicians.findTechnician(command.technicianId()).orElse(null);

		Instant now = clock.instant();
		Cancellation cancellation = request.status() == RequestStatus.CANCELADA
				? requests.lastCancellation(request.id()).orElse(null) : null;
		WorkflowSubject subject = new WorkflowSubject(request.status(), request.requesterId(), request.areaId(),
				request.technicianId(), cancellation, now);
		TransitionOutcome outcome = StateMachine.transition(actor, subject, command, technician);

		// Restoring gives back the time the request spent cancelled: its SLA did not run meanwhile.
		ServiceRequest updated = cancellation != null
				? request.restoredTo(outcome.to(), Duration.between(cancellation.cancelledAt(), now), now)
				: request.transitionedTo(outcome.to(), outcome.technicianId(), outcome.closureReason(),
						outcome.solution(), now);
		RequestHistoryEntry entry = new RequestHistoryEntry(request.status(), outcome.to(), actor.id(),
				outcome.historyTechnicianId(), outcome.reason(), now);
		ServiceRequest result = requests.update(updated, entry);
		if (eventPublisher != null) {
			eventPublisher.publishEvent(TransitionEvents.of(request.status(), result, actor.id()));
		}
		return result;
	}

}
