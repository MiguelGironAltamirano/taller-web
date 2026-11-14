package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;
import java.util.Optional;

/** Read side of the module: queries that already join the names the API returns. */
public interface RequestQueries {

	/** A page of the requests the scope can see, newest first, narrowed by the filter. */
	PageView<RequestSummaryView> search(RequestScope scope, RequestFilter filter, int page, int size, Instant now);

	/** The detail without visibility checks; callers must apply the access rule. */
	Optional<RequestDetailView> findDetail(String code, Instant now);

}
