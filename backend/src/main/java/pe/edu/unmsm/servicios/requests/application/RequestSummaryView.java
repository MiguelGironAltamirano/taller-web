package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** What a list row needs: the contract summary plus the names the inboxes display. */
public record RequestSummaryView(String code, String categoryName, String locationName, String priorityName,
		RequestStatus status, Instant createdAt, Instant dueAt, boolean overdue, String description,
		String requesterName, String areaName, String technicianName, String facultyName, String pavilionName,
		String roomName) {
}
