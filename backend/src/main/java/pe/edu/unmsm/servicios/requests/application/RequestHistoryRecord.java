package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** One state change of a request, flattened for read-only consumers outside the requests module. */
public record RequestHistoryRecord(long id, Instant createdAt, String actorName, String requestCode,
		RequestStatus fromStatus, RequestStatus toStatus, String reason) {
}
