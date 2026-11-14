package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Optional narrowing of the request list; a {@code null} field does not filter. {@code from} and {@code to} are
 * inclusive calendar days of the registration date, counted in Lima time like the year of the code.
 */
public record RequestFilter(RequestStatus status, LocalDate from, LocalDate to, Long areaId, Boolean overdue) {

	public static final RequestFilter NONE = new RequestFilter(null, null, null, null, null);

	static final ZoneId ZONE = ZoneId.of("America/Lima");

	/** First instant included, or {@code null} when there is no lower bound. */
	public Instant createdFrom() {
		return from == null ? null : from.atStartOfDay(ZONE).toInstant();
	}

	/** First instant excluded, or {@code null} when there is no upper bound. */
	public Instant createdBefore() {
		return to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant();
	}

}
