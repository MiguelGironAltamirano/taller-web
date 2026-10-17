package pe.edu.unmsm.servicios.requests.application;

import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * Which requests a user can list. It mirrors {@code AccessPolicy.canViewRequest}: the requester sees their own,
 * a supervisor those of the area, a technician those assigned, and an administrator all of them.
 */
public record RequestScope(String role, long userId, Long areaId) {

	public static RequestScope of(AuthenticatedUser user) {
		return new RequestScope(user.role(), user.id(), user.areaId());
	}

}
