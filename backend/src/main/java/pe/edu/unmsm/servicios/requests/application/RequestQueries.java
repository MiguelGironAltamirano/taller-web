package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;
import java.util.Optional;

/** Read side of the module: queries that already join the names the API returns. */
public interface RequestQueries {

	/** A page of the requests the scope can see that match the criteria, in the order it asks for. */
	PageView<RequestSummaryView> search(RequestScope scope, RequestListCriteria criteria, Instant now);

	/** Totals of the requests the scope can see, without other filters (the inbox counters). */
	RequestCountsView count(RequestScope scope, Instant now);

	/** The detail without visibility checks; callers must apply the access rule. */
	Optional<RequestDetailView> findDetail(String code, Instant now);

}
