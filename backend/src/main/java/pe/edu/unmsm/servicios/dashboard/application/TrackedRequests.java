package pe.edu.unmsm.servicios.dashboard.application;

import java.time.Instant;
import java.util.List;

import pe.edu.unmsm.servicios.dashboard.domain.TrackedRequest;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsRow;

/** Translates the rows of the requests module into the dashboard model, applying its rules at {@code now}. */
final class TrackedRequests {

	private TrackedRequests() {
	}

	static List<TrackedRequest> from(List<RequestAnalyticsRow> rows, Instant now) {
		return rows.stream().map(row -> from(row, now)).toList();
	}

	static TrackedRequest from(RequestAnalyticsRow row, Instant now) {
		return new TrackedRequest(row.code(), row.status().name(), row.categoryName(), row.priorityName(),
				row.locationName(), row.technicianId(), row.technicianName(), row.createdAt(), row.dueAt(),
				row.wasAttended() ? row.firstResolvedAt() : null, row.isOpen(), row.isOverdue(now));
	}

}
