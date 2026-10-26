package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * The detail of a request for the user who asks. The ports are implemented by the modules that own each part:
 * workflow (transitions and actions) and collaboration (comments and evidences).
 */
public class GetRequest {

	private final RequestQueries queries;
	private final AllowedTransitions allowedTransitions;
	private final AllowedActions allowedActions;
	private final RequestCollaboration collaboration;
	private final Clock clock;

	public GetRequest(RequestQueries queries, AllowedTransitions allowedTransitions, AllowedActions allowedActions,
			RequestCollaboration collaboration, Clock clock) {
		this.queries = queries;
		this.allowedTransitions = allowedTransitions;
		this.allowedActions = allowedActions;
		this.collaboration = collaboration;
		this.clock = clock;
	}

	/** A request the user may not see is reported as not found, so its existence is not revealed. */
	public RequestDetailView execute(AuthenticatedUser user, String code) {
		RequestDetailView detail = queries.findDetail(code, clock.instant())
				.filter(view -> AccessPolicy.canViewRequest(user, view.requesterId(), view.areaId(),
						view.technicianId()))
				.orElseThrow(() -> new RequestNotFoundException(code));
		return detail.withPermissions(allowedTransitions.forUser(user, detail), actionsFor(user, detail))
				.withCollaboration(collaboration.comments(detail.id()), collaboration.evidences(detail.id()));
	}

	/** The workflow actions plus the priority change, which this module owns (see {@link ChangePriority}). */
	private List<RequestAction> actionsFor(AuthenticatedUser user, RequestDetailView detail) {
		List<RequestAction> actions = new ArrayList<>(allowedActions.forUser(user, detail));
		if (ChangePriority.mayChange(user, detail.areaId(), detail.summary().status())) {
			actions.add(RequestAction.PRIORITY);
		}
		return actions;
	}

}
