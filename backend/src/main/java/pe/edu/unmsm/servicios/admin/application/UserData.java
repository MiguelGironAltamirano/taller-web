package pe.edu.unmsm.servicios.admin.application;

import java.util.Locale;

import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;

/** Editable fields of an account, as sent by the administrator. */
public record UserData(String fullName, String email, Role role, Long areaId, RequesterType requesterType) {

	UserData normalized() {
		return new UserData(fullName.trim(), email.trim().toLowerCase(Locale.ROOT), role, areaId, requesterType);
	}

}
