package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** One history entry as displayed; {@code note} is only set for changes that keep the status. */
public record HistoryView(RequestStatus fromStatus, RequestStatus toStatus, String actorName, String reason,
		Instant createdAt, String note) {
}
