package pe.edu.unmsm.servicios.workflow.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.workflow.application.TechnicianView;

final class WorkflowDtos {

	private WorkflowDtos() {
	}

	/**
	 * {@code { to, reason?, technicianId? }} of the API contract. {@code closureReason} tells "No procede" from
	 * "Duplicada" when closing without resolving; the contract also lets that value travel in {@code reason}.
	 */
	record TransitionBody(
			@NotNull(message = "Indica el estado de destino") RequestStatus to,
			String reason,
			Long technicianId,
			ClosureReason closureReason) {
	}

	record TechnicianResponse(long id, String fullName, long activeRequests) {

		static TechnicianResponse from(TechnicianView view) {
			return new TechnicianResponse(view.id(), view.fullName(), view.activeRequests());
		}

	}

}
