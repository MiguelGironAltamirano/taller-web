package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;

import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class GetRequest {

	private final RequestQueries queries;
	private final AllowedTransitions allowedTransitions;
	private final Clock clock;

	public GetRequest(RequestQueries queries, AllowedTransitions allowedTransitions, Clock clock) {
		this.queries = queries;
		this.allowedTransitions = allowedTransitions;
		this.clock = clock;
	}

	/** A request the user may not see is reported as not found, so its existence is not revealed. */
	public RequestDetailView execute(AuthenticatedUser user, String code) {
		RequestDetailView detail = queries.findDetail(code, clock.instant())
				.filter(view -> AccessPolicy.canViewRequest(user, view.requesterId(), view.areaId(),
						view.technicianId()))
				.orElseThrow(() -> new RequestNotFoundException(code));
		return detail.withAllowedTransitions(allowedTransitions.forUser(user, detail));
	}

}
