package pe.edu.unmsm.servicios.requests.domain;

import java.time.Duration;
import java.time.Instant;

/** SLA rules: the due date comes from the priority; overdue means not resolved before it. */
public final class Sla {

	private Sla() {
	}

	/** Due date = registration date + the SLA hours of the priority. */
	public static Instant dueAt(Instant createdAt, int slaHours) {
		return createdAt.plus(Duration.ofHours(slaHours));
	}

	public static boolean isOverdue(RequestStatus status, Instant dueAt, Instant now) {
		return !status.isFinished() && dueAt.isBefore(now);
	}

}
