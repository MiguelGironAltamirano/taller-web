package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;


/** Closes requests that have been in RESUELTA for more than 72 hours without a rating from the requester. */
public class AutoCloseResolvedRequests {

	static final int AUTO_CLOSE_HOURS = 72;

	private final ResolvedRequestStore store;
	private final Clock clock;

	public AutoCloseResolvedRequests(ResolvedRequestStore store, Clock clock) {
		this.store = store;
		this.clock = clock;
	}

	public void run() {
		Instant now = clock.instant();
		List<Long> ids = store.findResolvedWithoutRatingOlderThan(AUTO_CLOSE_HOURS, now);
		for (long id : ids) {
			store.closeWithoutRating(id, now);
		}
	}

}
