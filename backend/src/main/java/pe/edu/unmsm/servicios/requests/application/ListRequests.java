package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;

import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/** The inbox of the user: filtered, ordered and paged on the server, always within the role visibility. */
public class ListRequests {

	private final RequestQueries queries;
	private final Clock clock;

	public ListRequests(RequestQueries queries, Clock clock) {
		this.queries = queries;
		this.clock = clock;
	}

	/**
	 * The criteria were already validated by {@link RequestListCriteria#parse}. The requester gets the rows as they
	 * see them (see {@link RequestSummaryView#asSeenByRequester()}).
	 */
	public PageView<RequestSummaryView> execute(AuthenticatedUser user, RequestListCriteria criteria) {
		if (!AccessPolicy.isRequester(user)) {
			return queries.search(RequestScope.of(user), criteria, clock.instant());
		}
		PageView<RequestSummaryView> page = queries.search(RequestScope.of(user), criteria.forRequester(),
				clock.instant());
		return new PageView<>(page.items().stream().map(RequestSummaryView::asSeenByRequester).toList(), page.page(),
				page.size(), page.total());
	}

	/** Counters of the inbox (total, overdue and per status) over everything the user can see. */
	public RequestCountsView counts(AuthenticatedUser user) {
		RequestCountsView counts = queries.count(RequestScope.of(user), clock.instant());
		return AccessPolicy.isRequester(user) ? new RequestCountsView(counts.total(), 0, counts.byStatus()) : counts;
	}

}
