package pe.edu.unmsm.servicios.requests.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record ServiceRequest(Long id, String code, long requesterId, long categoryId, long locationId,
		String description, long priorityId, RequestStatus status, long areaId, Long technicianId,
		ClosureReason closureReason, Instant createdAt, Instant dueAt, Instant updatedAt) {

	public static final int DESCRIPTION_MIN = 20;
	public static final int DESCRIPTION_MAX = 2000;

	/** Why the description is not acceptable, or empty when it is. Spaces around the text do not count. */
	public static Optional<String> descriptionError(String description) {
		int length = description == null ? 0 : description.strip().length();
		if (length < DESCRIPTION_MIN || length > DESCRIPTION_MAX) {
			return Optional.of("La descripción debe tener entre " + DESCRIPTION_MIN + " y " + DESCRIPTION_MAX
					+ " caracteres");
		}
		return Optional.empty();
	}

	/**
	 * Registers a new request: it starts as {@code REGISTRADA}, unassigned, and falls due after the SLA hours of
	 * its priority.
	 */
	public static ServiceRequest register(String code, long requesterId, long categoryId, long areaId, long locationId,
			String description, long priorityId, int slaHours, Instant now) {
		descriptionError(description).ifPresent(message -> {
			throw new InvalidRequestException(Map.of("description", message));
		});
		return new ServiceRequest(null, code, requesterId, categoryId, locationId, description.strip(), priorityId,
				RequestStatus.REGISTRADA, areaId, null, null, now, Sla.dueAt(now, slaHours), now);
	}

	public boolean isOverdue(Instant now) {
		return Sla.isOverdue(status, dueAt, now);
	}

	/** The same request after a transition; creation data and the due date do not change. */
	public ServiceRequest transitionedTo(RequestStatus newStatus, Long newTechnicianId, ClosureReason newClosureReason,
			Instant now) {
		return new ServiceRequest(id, code, requesterId, categoryId, locationId, description, priorityId, newStatus,
				areaId, newTechnicianId, newClosureReason, createdAt, dueAt, now);
	}

}
