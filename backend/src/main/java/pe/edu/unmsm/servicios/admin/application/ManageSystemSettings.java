package pe.edu.unmsm.servicios.admin.application;

import java.util.Map;

import pe.edu.unmsm.servicios.admin.domain.InvalidSettingsException;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;

/** Reads and changes the global system settings (administration only). */
public class ManageSystemSettings {

	private final SystemSettingsStore store;

	public ManageSystemSettings(SystemSettingsStore store) {
		this.store = store;
	}

	public SystemSettings current() {
		return store.current();
	}

	public SystemSettings update(long actorId, int autoCloseHours, boolean emailNotifications,
			boolean triageSuggestions) {
		if (autoCloseHours <= 0) {
			throw new InvalidSettingsException(Map.of("autoCloseHours", "Ingresa horas mayores a 0."));
		}
		SystemSettings settings = new SystemSettings(autoCloseHours, emailNotifications, triageSuggestions);
		store.save(settings, actorId);
		return settings;
	}

}
