package pe.edu.unmsm.servicios.workflow.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.domain.StateMachine;
import pe.edu.unmsm.servicios.workflow.domain.Technician;
import pe.edu.unmsm.servicios.workflow.domain.TransitionCommand;
import pe.edu.unmsm.servicios.workflow.domain.TransitionOutcome;
import pe.edu.unmsm.servicios.workflow.domain.WorkflowSubject;

public class TransitionRequest {

	private final RequestStateStore requests;
	private final TechnicianDirectory technicians;
	private final Clock clock;

	public TransitionRequest(RequestStateStore requests, TechnicianDirectory technicians, Clock clock) {
		this.requests = requests;
		this.technicians = technicians;
		this.clock = clock;
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

		WorkflowSubject subject = new WorkflowSubject(request.status(), request.requesterId(), request.areaId(),
				request.technicianId());
		TransitionOutcome outcome = StateMachine.transition(actor, subject, command, technician);

		Instant now = clock.instant();
		ServiceRequest updated = request.transitionedTo(outcome.to(), outcome.technicianId(), outcome.closureReason(),
				now);
		RequestHistoryEntry entry = new RequestHistoryEntry(request.status(), outcome.to(), actor.id(),
				outcome.historyTechnicianId(), outcome.reason(), now);
		return requests.update(updated, entry);
	}

}
