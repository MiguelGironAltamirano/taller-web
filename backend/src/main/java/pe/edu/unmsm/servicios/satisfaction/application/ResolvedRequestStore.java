package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Instant;
import java.util.List;

/** Queries and mutations needed by the auto-close scheduler. */
public interface ResolvedRequestStore {

	/** Returns the ids of requests in RESUELTA status resolved more than {@code hours} hours ago without a rating. */
	List<Long> findResolvedWithoutRatingOlderThan(int hours, Instant now);

	/** Closes the request (RESUELTA → CERRADA) with closure reason CONFIRMADA (auto-close). */
	void closeWithoutRating(long requestId, Instant now);

}
