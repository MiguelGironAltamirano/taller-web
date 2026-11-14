package pe.edu.unmsm.servicios.workflow.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.application.UnconfirmedResolutions;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.workflow.domain.StateMachine;
import pe.edu.unmsm.servicios.workflow.domain.TransitionOutcome;
import pe.edu.unmsm.servicios.workflow.domain.WorkflowSubject;

/**
 * Closes one request whose solution the requester did not answer in time, in its own transaction. The request is
 * locked and checked again before closing, so a confirmation, a rejection or another run of the job that got there
 * first wins and nothing is done twice.
 */
public class CloseUnconfirmedRequest {

	private final RequestStateStore requests;
	private final UnconfirmedResolutions resolutions;
	private final Clock clock;
	private final ApplicationEventPublisher eventPublisher;

	public CloseUnconfirmedRequest(RequestStateStore requests, UnconfirmedResolutions resolutions, Clock clock,
			ApplicationEventPublisher eventPublisher) {
		this.requests = requests;
		this.resolutions = resolutions;
		this.clock = clock;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Closes the request when it is still {@code RESUELTA} and entered that status at or before {@code cutoff}.
	 *
	 * @param hours the confirmation period, recorded in the history text
	 * @return the closed request, or empty when it no longer qualifies (or does not exist)
	 */
	@Transactional
	public Optional<ServiceRequest> execute(String code, Instant cutoff, int hours) {
		Optional<ServiceRequest> found = requests.findByCodeForUpdate(code);
		if (found.isEmpty() || found.get().status() != RequestStatus.RESUELTA) {
			return Optional.empty();
		}
		Optional<Instant> resolvedAt = resolutions.resolvedAt(code);
		if (resolvedAt.isEmpty() || resolvedAt.get().isAfter(cutoff)) {
			return Optional.empty();
		}
		ServiceRequest request = found.get();
		TransitionOutcome outcome = StateMachine.autoClose(new WorkflowSubject(request.status(), request.requesterId(),
				request.areaId(), request.technicianId()), hours);

		Instant now = clock.instant();
		ServiceRequest updated = request.transitionedTo(outcome.to(), outcome.technicianId(), outcome.closureReason(),
				outcome.solution(), now);
		RequestHistoryEntry entry = new RequestHistoryEntry(request.status(), outcome.to(), null,
				outcome.historyTechnicianId(), outcome.reason(), now);
		ServiceRequest result = requests.update(updated, entry);
		eventPublisher.publishEvent(TransitionEvents.of(request.status(), result, null));
		return Optional.of(result);
	}

}
