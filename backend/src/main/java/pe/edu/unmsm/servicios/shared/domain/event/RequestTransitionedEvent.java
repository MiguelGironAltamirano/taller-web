package pe.edu.unmsm.servicios.shared.domain.event;

/**
 * Published after a request changes status. {@code actorId} is null when the system, not a user, made the change
 * (the automatic closure of an unconfirmed solution); then nobody is excluded from the notifications.
 * {@code fromStatus} is the status before the change, so an assignment from {@code ASIGNADA} is a reassignment.
 */
public record RequestTransitionedEvent(
		String code,
		long requestId,
		long requesterId,
		long areaId,
		Long technicianId,
		Long actorId,
		String fromStatus,
		String toStatus,
		boolean isAssignment
) {

	/** Whether the system made the change on its own (no user acted). */
	public boolean bySystem() {
		return actorId == null;
	}

	/** Whether the request was already assigned and now goes to another technician (ASIGNADA → ASIGNADA). */
	public boolean isReassignment() {
		return isAssignment && "ASIGNADA".equals(fromStatus);
	}

}
