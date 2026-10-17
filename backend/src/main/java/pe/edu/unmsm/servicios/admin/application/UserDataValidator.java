package pe.edu.unmsm.servicios.admin.application;

import java.util.LinkedHashMap;
import java.util.Map;

import pe.edu.unmsm.servicios.admin.domain.InvalidUserDataException;
import pe.edu.unmsm.servicios.admin.domain.UserAccountRules;

/** Applies the account rules plus the area lookup, returning normalized data ready to persist. */
final class UserDataValidator {

	private UserDataValidator() {
	}

	static UserData validate(UserData data, AreaDirectory areas) {
		Map<String, String> errors = new LinkedHashMap<>(UserAccountRules.violations(data.role(), data.areaId()));
		if (errors.isEmpty() && data.areaId() != null && !areas.isActiveArea(data.areaId())) {
			errors.put("areaId", "El área no existe o está inactiva");
		}
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(errors);
		}
		UserData normalized = data.normalized();
		return new UserData(normalized.fullName(), normalized.email(), normalized.role(), normalized.areaId(),
				UserAccountRules.requesterTypeFor(normalized.role(), normalized.requesterType()));
	}

}
