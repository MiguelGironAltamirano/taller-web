package pe.edu.unmsm.servicios.requests.domain;

import java.time.Instant;

/**
 * One change in the life of a request; {@code fromStatus} is null for the registration itself. {@code note}
 * describes a change that keeps the status (for example {@code Prioridad Alta → Prioridad Media}); state changes
 * leave it null. {@code actorId} is null when the system, not a user, made the change (automatic closure).
 */
public record RequestHistoryEntry(RequestStatus fromStatus, RequestStatus toStatus, Long actorId, Long technicianId,
		String reason, Instant createdAt, String note) {

	/** A state change, without note. */
	public RequestHistoryEntry(RequestStatus fromStatus, RequestStatus toStatus, Long actorId, Long technicianId,
			String reason, Instant createdAt) {
		this(fromStatus, toStatus, actorId, technicianId, reason, createdAt, null);
	}

}
