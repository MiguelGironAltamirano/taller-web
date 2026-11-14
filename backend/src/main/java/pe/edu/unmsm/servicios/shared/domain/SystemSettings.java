package pe.edu.unmsm.servicios.shared.domain;

/**
 * Global, administrator-managed system settings shared by every module.
 *
 * @param autoCloseHours hours after which a RESUELTA request is closed automatically (always greater than 0)
 * @param emailNotifications whether notifications are also delivered by email
 * @param triageSuggestions whether category and priority are suggested while a request is registered
 */
public record SystemSettings(int autoCloseHours, boolean emailNotifications, boolean triageSuggestions) {

	public SystemSettings {
		if (autoCloseHours <= 0) {
			throw new IllegalArgumentException("autoCloseHours must be greater than 0");
		}
	}

}
