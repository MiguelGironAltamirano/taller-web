package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.Sla;

/**
 * One request as reporting needs it: flat, with the names already resolved. {@code technicianId} and
 * {@code technicianName} are null while unassigned; {@code firstResolvedAt} is the first time it reached RESUELTA
 * according to its history, or null if it never did.
 */
public record RequestAnalyticsRow(String code, RequestStatus status, String categoryName, String priorityName,
		long areaId, String areaName, Long technicianId, String technicianName, String locationName,
		Instant createdAt, Instant dueAt, Instant firstResolvedAt) {

	/** Neither RESUELTA nor CERRADA today. */
	public boolean isOpen() {
		return !status.isFinished();
	}

	/** The same rule as the {@code overdue} flag of the request list. */
	public boolean isOverdue(Instant now) {
		return Sla.isOverdue(status, dueAt, now);
	}

	/**
	 * It reached RESUELTA and is still resolved or closed: a request reopened after its resolution is pending again,
	 * and one closed as NO_PROCEDE or DUPLICADA never was attended.
	 */
	public boolean wasAttended() {
		return firstResolvedAt != null && status.isFinished();
	}

}
