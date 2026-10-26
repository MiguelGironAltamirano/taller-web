package pe.edu.unmsm.servicios.shared.application;

import pe.edu.unmsm.servicios.shared.domain.SystemSettings;

/**
 * Read-only port to the global system settings. Any module (for example the workflow auto-close job or
 * triage suggestions) injects this interface; the administration module provides the only implementation
 * and owns how the settings are stored and changed.
 *
 * <p>Every call returns the latest persisted values, so consumers call it when they need the settings
 * instead of caching the result.
 */
public interface SystemSettingsProvider {

	SystemSettings current();

}
