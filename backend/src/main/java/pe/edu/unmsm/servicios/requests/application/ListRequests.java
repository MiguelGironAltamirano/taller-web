package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class ListRequests {

	public static final int MAX_PAGE_SIZE = 500;

	private final RequestQueries queries;
	private final Clock clock;

	public ListRequests(RequestQueries queries, Clock clock) {
		this.queries = queries;
		this.clock = clock;
	}

	/** Lists the requests the user can see; out-of-range paging values are clamped rather than rejected. */
	public PageView<RequestSummaryView> execute(AuthenticatedUser user, RequestStatus status, int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
		return queries.search(RequestScope.of(user), status, safePage, safeSize, clock.instant());
	}

}
