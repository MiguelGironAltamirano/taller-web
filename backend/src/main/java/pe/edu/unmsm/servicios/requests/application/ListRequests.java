package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;

import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/** The inbox of the user: filtered, ordered and paged on the server, always within the role visibility. */
public class ListRequests {

	private final RequestQueries queries;
	private final Clock clock;

	public ListRequests(RequestQueries queries, Clock clock) {
		this.queries = queries;
		this.clock = clock;
	}

	/** The criteria were already validated by {@link RequestListCriteria#parse}. */
	public PageView<RequestSummaryView> execute(AuthenticatedUser user, RequestListCriteria criteria) {
		return queries.search(RequestScope.of(user), criteria, clock.instant());
	}

	/** Counters of the inbox (total, overdue and per status) over everything the user can see. */
	public RequestCountsView counts(AuthenticatedUser user) {
		return queries.count(RequestScope.of(user), clock.instant());
	}

}
