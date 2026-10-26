package pe.edu.unmsm.servicios.shared.domain.event;

/**
 * Published after a request changes status. {@code actorId} is null when the system, not a user, made the change
 * (the automatic closure of an unconfirmed solution); then nobody is excluded from the notifications.
 */
public record RequestTransitionedEvent(
		String code,
		long requestId,
		long requesterId,
		long areaId,
		Long technicianId,
		Long actorId,
		String toStatus,
		boolean isAssignment
) {

	/** Whether the system made the change on its own (no user acted). */
	public boolean bySystem() {
		return actorId == null;
	}

}
