package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Full detail of a request. The ids are kept so the visibility rule can be applied; the controller never
 * exposes them beyond what the contract defines. What depends on the user who asks ({@code allowedTransitions},
 * {@code allowedActions}), on the system settings ({@code autoCloseAt}, null unless the request is RESUELTA) and
 * what other modules own ({@code comments}, {@code evidences}, {@code rating}, which is null until the requester
 * rates it) is filled in by {@link GetRequest}.
 */
public record RequestDetailView(long id, RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
		ClosureReason closureReason, String solution, List<HistoryView> history,
		List<RequestStatus> allowedTransitions, List<RequestAction> allowedActions, List<CommentView> comments,
		List<EvidenceView> evidences, RatingView rating, Instant autoCloseAt) {

	/** The detail as read from storage, before adding what depends on the user or on other modules. */
	public RequestDetailView(long id, RequestSummaryView summary, long requesterId, long areaId, Long technicianId,
			ClosureReason closureReason, String solution, List<HistoryView> history) {
		this(id, summary, requesterId, areaId, technicianId, closureReason, solution, history, List.of(), List.of(),
				List.of(), List.of(), null, null);
	}

	/**
	 * When the request entered RESUELTA for the last time (a rejected solution sends it back to EN_ATENCION and a
	 * new resolution restarts the confirmation period); empty unless it is RESUELTA now. Same rule as the closure
	 * job ({@link UnconfirmedResolutions}): RESUELTA → RESUELTA entries (notes) do not count.
	 */
	public Optional<Instant> resolvedAt() {
		if (summary.status() != RequestStatus.RESUELTA) {
			return Optional.empty();
		}
		return history.stream()
				.filter(entry -> entry.toStatus() == RequestStatus.RESUELTA
						&& entry.fromStatus() != RequestStatus.RESUELTA)
				.map(HistoryView::createdAt)
				.max(Instant::compareTo);
	}

	/** See {@link RequestSummaryView#asSeenByRequester()}; the internal history entries are dropped too. */
	public RequestDetailView asSeenByRequester() {
		return new RequestDetailView(id, summary.asSeenByRequester(), requesterId, areaId, technicianId, closureReason,
				solution, history.stream().filter(entry -> !entry.internal()).toList(), allowedTransitions,
				allowedActions, comments, evidences, rating, autoCloseAt);
	}

	public RequestDetailView withPermissions(List<RequestStatus> transitions, List<RequestAction> actions) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				List.copyOf(transitions), List.copyOf(actions), comments, evidences, rating, autoCloseAt);
	}

	public RequestDetailView withCollaboration(List<CommentView> newComments, List<EvidenceView> newEvidences) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				allowedTransitions, allowedActions, List.copyOf(newComments), List.copyOf(newEvidences), rating,
				autoCloseAt);
	}

	public RequestDetailView withRating(RatingView newRating) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				allowedTransitions, allowedActions, comments, evidences, newRating, autoCloseAt);
	}

	public RequestDetailView withAutoCloseAt(Instant newAutoCloseAt) {
		return new RequestDetailView(id, summary, requesterId, areaId, technicianId, closureReason, solution, history,
				allowedTransitions, allowedActions, comments, evidences, rating, newAutoCloseAt);
	}

}
