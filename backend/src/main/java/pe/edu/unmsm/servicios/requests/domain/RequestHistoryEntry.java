package pe.edu.unmsm.servicios.requests.domain;

import java.time.Instant;

/** One change in the life of a request; {@code fromStatus} is null for the registration itself. */
public record RequestHistoryEntry(RequestStatus fromStatus, RequestStatus toStatus, long actorId, Long technicianId,
		String reason, Instant createdAt) {
}
