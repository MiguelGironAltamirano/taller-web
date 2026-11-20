package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * What a list row needs: the contract summary plus the names the inboxes display. {@code priorityName} and
 * {@code dueAt} are the current priority and due date the staff works with; {@code requestedPriorityName} and
 * {@code requestedDueAt} are the priority the requester chose and the estimated date it gave at registration.
 * {@code estimatedDue} tells that {@code dueAt} is that estimated date (the requester's view).
 */
public record RequestSummaryView(String code, String categoryName, String locationName, String priorityName,
		RequestStatus status, Instant createdAt, Instant dueAt, boolean overdue, String description,
		String requesterName, String areaName, String technicianName, String facultyName, String pavilionName,
		String roomName, String requestedPriorityName, Instant requestedDueAt, boolean estimatedDue) {

	/** A request whose priority never changed. */
	public RequestSummaryView(String code, String categoryName, String locationName, String priorityName,
			RequestStatus status, Instant createdAt, Instant dueAt, boolean overdue, String description,
			String requesterName, String areaName, String technicianName, String facultyName, String pavilionName,
			String roomName) {
		this(code, categoryName, locationName, priorityName, status, createdAt, dueAt, overdue, description,
				requesterName, areaName, technicianName, facultyName, pavilionName, roomName, priorityName, dueAt,
				false);
	}

	/**
	 * The requester sees the priority they chose and its estimated date, never as overdue, so a supervisor's change
	 * goes unnoticed.
	 */
	public RequestSummaryView asSeenByRequester() {
		return new RequestSummaryView(code, categoryName, locationName, requestedPriorityName, status, createdAt,
				requestedDueAt, false, description, requesterName, areaName, technicianName, facultyName,
				pavilionName, roomName, requestedPriorityName, requestedDueAt, true);
	}

}
