package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Full detail of a request. The ids are kept so the visibility rule can be applied; the controller never
 * exposes them beyond what the contract defines.
 */
public record RequestDetailView(long id, RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
		ClosureReason closureReason, String solution, List<HistoryView> history,
		List<RequestStatus> allowedTransitions) {

	public RequestDetailView withAllowedTransitions(List<RequestStatus> transitions) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				List.copyOf(transitions));
	}

}
