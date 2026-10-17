package pe.edu.unmsm.servicios.admin.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;

/** Rules for accounts created or edited by an administrator (spec §4 and §7). */
public final class UserAccountRules {

	private UserAccountRules() {
	}

	/** Field errors for the role and area combination; empty when it is valid. */
	public static Map<String, String> violations(Role role, Long areaId) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (role == null) {
			errors.put("role", "El rol es obligatorio");
		}
		else if (belongsToArea(role) && areaId == null) {
			errors.put("areaId", "El área es obligatoria para este rol");
		}
		else if (!belongsToArea(role) && areaId != null) {
			errors.put("areaId", "Este rol no pertenece a un área");
		}
		return errors;
	}

	/** Requester type only applies to requesters, who default to students. */
	public static RequesterType requesterTypeFor(Role role, RequesterType requested) {
		if (role != Role.SOLICITANTE) {
			return null;
		}
		return requested == null ? RequesterType.ESTUDIANTE : requested;
	}

	public static boolean belongsToArea(Role role) {
		return role == Role.TECNICO || role == Role.SUPERVISOR;
	}

}
