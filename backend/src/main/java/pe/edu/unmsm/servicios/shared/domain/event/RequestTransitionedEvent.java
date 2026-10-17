package pe.edu.unmsm.servicios.shared.domain.event;

public record RequestTransitionedEvent(
		String code,
		long requestId,
		long requesterId,
		long areaId,
		Long technicianId,
		long actorId,
		String toStatus,
		boolean isAssignment
) {
}
