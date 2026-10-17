package pe.edu.unmsm.servicios.admin.domain;

import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;

/** A user account as the administrator sees it; never carries the password hash. */
public record ManagedUser(long id, String fullName, String email, Role role, Long areaId, String areaName,
		RequesterType requesterType, boolean active) {
}
