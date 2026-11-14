package pe.edu.unmsm.servicios.admin.application;

import pe.edu.unmsm.servicios.shared.application.SystemSettingsProvider;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;

/** Administration-owned persistence of the system settings; it also serves the shared read-only port. */
public interface SystemSettingsStore extends SystemSettingsProvider {

	void save(SystemSettings settings, long updatedBy);

}
