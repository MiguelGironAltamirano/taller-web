package pe.edu.unmsm.servicios.admin.infrastructure.persistence;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.admin.application.SystemSettingsStore;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;

/**
 * Single-row {@code system_settings} table (id = 1, seeded by its migration). This bean is the only
 * implementation of the shared {@link pe.edu.unmsm.servicios.shared.application.SystemSettingsProvider} port.
 */
@Repository
class JdbcSystemSettingsStore implements SystemSettingsStore {

	private final JdbcTemplate jdbc;
	private final Clock clock;

	JdbcSystemSettingsStore(JdbcTemplate jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Override
	public SystemSettings current() {
		return jdbc.queryForObject("""
				select auto_close_hours, email_notifications, triage_suggestions
				  from system_settings
				 where id = 1
				""", (rs, row) -> new SystemSettings(rs.getInt("auto_close_hours"),
				rs.getBoolean("email_notifications"), rs.getBoolean("triage_suggestions")));
	}

	@Override
	public void save(SystemSettings settings, long updatedBy) {
		jdbc.update("""
				update system_settings
				   set auto_close_hours = ?, email_notifications = ?, triage_suggestions = ?,
				       updated_by = ?, updated_at = ?
				 where id = 1
				""", settings.autoCloseHours(), settings.emailNotifications(), settings.triageSuggestions(),
				updatedBy, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
	}

}
