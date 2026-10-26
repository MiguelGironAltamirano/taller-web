package pe.edu.unmsm.servicios.dashboard.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * A request as the dashboard sees it. {@code attendedAt} is the first time it reached RESUELTA, set only while it is
 * still resolved or closed; {@code open} means neither RESUELTA nor CERRADA today; {@code overdue} follows the rule
 * of the request list at the moment of the query.
 */
public record TrackedRequest(String code, String status, String categoryName, String priorityName,
		String locationName, Long technicianId, String technicianName, Instant createdAt, Instant dueAt,
		Instant attendedAt, boolean open, boolean overdue) {

	public boolean attended() {
		return attendedAt != null;
	}

	public boolean attendedOnTime() {
		return attended() && attendedAt.isBefore(dueAt);
	}

	/** Hours from registration to the first resolution; only meaningful when {@link #attended()}. */
	public double attentionHours() {
		return Duration.between(createdAt, attendedAt).toMillis() / 3_600_000.0;
	}

}
