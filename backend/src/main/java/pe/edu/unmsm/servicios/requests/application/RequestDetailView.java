package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Full detail of a request. The ids are kept so the visibility rule can be applied; the controller never
 * exposes them beyond what the contract defines. What depends on the user who asks ({@code allowedTransitions},
 * {@code allowedActions}) and what other modules own ({@code comments}, {@code evidences}) is filled in by
 * {@link GetRequest}.
 */
public record RequestDetailView(long id, RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
		ClosureReason closureReason, String solution, List<HistoryView> history,
		List<RequestStatus> allowedTransitions, List<RequestAction> allowedActions, List<CommentView> comments,
		List<EvidenceView> evidences) {

	/** The detail as read from storage, before adding what depends on the user or on other modules. */
	public RequestDetailView(long id, RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
			ClosureReason closureReason, String solution, List<HistoryView> history) {
		this(id, summary, requesterId, areaId, technicianId, closureReason, solution, history, List.of(), List.of(),
				List.of(), List.of());
	}

	public RequestDetailView withPermissions(List<RequestStatus> transitions, List<RequestAction> actions) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				List.copyOf(transitions), List.copyOf(actions), comments, evidences);
	}

	public RequestDetailView withCollaboration(List<CommentView> newComments, List<EvidenceView> newEvidences) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				allowedTransitions, allowedActions, List.copyOf(newComments), List.copyOf(newEvidences));
	}

}
