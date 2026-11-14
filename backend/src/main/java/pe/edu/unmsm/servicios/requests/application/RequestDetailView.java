package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Full detail of a request. The ids are kept so the visibility rule can be applied; the controller never
 * exposes them beyond what the contract defines. {@code rating} is null until the requester rates.
 */
public record RequestDetailView(RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
		ClosureReason closureReason, List<HistoryView> history, List<RequestStatus> allowedTransitions,
		RatingView rating) {

	public RequestDetailView(RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
			ClosureReason closureReason, List<HistoryView> history, List<RequestStatus> allowedTransitions) {
		this(summary, requesterId, areaId, technicianId, closureReason, history, allowedTransitions, null);
	}

	public RequestDetailView withAllowedTransitions(List<RequestStatus> transitions) {
		return new RequestDetailView(summary, requesterId, areaId, technicianId, closureReason, history,
				List.copyOf(transitions), rating);
	}

}
