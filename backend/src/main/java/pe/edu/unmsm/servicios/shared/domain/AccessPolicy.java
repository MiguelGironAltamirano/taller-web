package pe.edu.unmsm.servicios.shared.domain;

/** Reusable, framework-free access rules shared by every module. */
public final class AccessPolicy {

	private static final String REQUESTER = "SOLICITANTE";
	private static final String SUPERVISOR = "SUPERVISOR";
	private static final String TECHNICIAN = "TECNICO";
	private static final String ADMINISTRATOR = "ADMINISTRADOR";

	private AccessPolicy() {
	}

	public static boolean isOwner(AuthenticatedUser user, long ownerId) {
		return user.id() == ownerId;
	}

	public static boolean isSupervisorOfArea(AuthenticatedUser user, long areaId) {
		return SUPERVISOR.equals(user.role()) && user.areaId() != null && user.areaId() == areaId;
	}

	public static boolean isAssignedTechnician(AuthenticatedUser user, Long technicianId) {
		return TECHNICIAN.equals(user.role()) && technicianId != null && user.id() == technicianId;
	}

	public static boolean isAdministrator(AuthenticatedUser user) {
		return ADMINISTRATOR.equals(user.role());
	}

	/** Visibility rule of the API contract: requester own, supervisor own area, technician assigned, admin all. */
	public static boolean canViewRequest(AuthenticatedUser user, long requesterId, long areaId, Long technicianId) {
		return isAdministrator(user)
				|| (REQUESTER.equals(user.role()) && isOwner(user, requesterId))
				|| isSupervisorOfArea(user, areaId)
				|| isAssignedTechnician(user, technicianId);
	}

}
