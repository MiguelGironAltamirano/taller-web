package pe.edu.unmsm.servicios.requests.application;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Allow-list of list orders; every order ends with a unique tie-breaker so paging is stable. */
public enum RequestSort {

	/** Most recently registered first (default). */
	NEWEST,
	/** Oldest registered first. */
	OLDEST,
	/** Pending requests first, each group by the closest due date. */
	DUE,
	/** Highest priority first, then the most recent. */
	PRIORITY;

	public String param() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Optional<RequestSort> fromParam(String value) {
		return Arrays.stream(values()).filter(sort -> sort.param().equals(value)).findFirst();
	}

}
