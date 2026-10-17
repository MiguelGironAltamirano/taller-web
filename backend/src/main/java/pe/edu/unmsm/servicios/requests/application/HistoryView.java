package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

public record HistoryView(RequestStatus fromStatus, RequestStatus toStatus, String actorName, String reason,
		Instant createdAt) {
}
