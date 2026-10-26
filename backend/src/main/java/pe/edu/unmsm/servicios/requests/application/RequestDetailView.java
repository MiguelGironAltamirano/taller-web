package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Full detail of a request. The ids are kept so the visibility rule can be applied; the controller never
 * exposes them beyond what the contract defines. What depends on the user who asks ({@code allowedTransitions},
 * {@code allowedActions}) is filled in by {@link GetRequest}.
 */
public record RequestDetailView(long id, RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
		ClosureReason closureReason, String solution, List<HistoryView> history,
		List<RequestStatus> allowedTransitions, List<RequestAction> allowedActions) {

	/** The detail as read from storage, before computing what the user may do. */
	public RequestDetailView(long id, RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
			ClosureReason closureReason, String solution, List<HistoryView> history) {
		this(id, summary, requesterId, areaId, technicianId, closureReason, solution, history, List.of(), List.of());
	}

	public RequestDetailView withPermissions(List<RequestStatus> transitions, List<RequestAction> actions) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				List.copyOf(transitions), List.copyOf(actions));
	}

}
