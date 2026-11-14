package pe.edu.unmsm.servicios.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.admin.domain.InvalidSettingsException;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;

class ManageSystemSettingsTest {

	private final InMemorySettingsStore store = new InMemorySettingsStore();
	private final ManageSystemSettings settings = new ManageSystemSettings(store);

	@Test
	void returnsTheCurrentSettings() {
		assertThat(settings.current()).isEqualTo(new SystemSettings(72, false, true));
	}

	@Test
	void savesValidSettingsAndRecordsWhoChangedThem() {
		SystemSettings saved = settings.update(7L, 48, true, false);

		assertThat(saved).isEqualTo(new SystemSettings(48, true, false));
		assertThat(store.current()).isEqualTo(saved);
		assertThat(store.updatedBy).isEqualTo(7L);
	}

	@Test
	void rejectsNonPositiveAutoCloseHoursWithoutSaving() {
		assertThatThrownBy(() -> settings.update(7L, 0, true, true))
				.isInstanceOfSatisfying(InvalidSettingsException.class,
						ex -> assertThat(ex.errors()).containsKey("autoCloseHours"));
		assertThatThrownBy(() -> settings.update(7L, -5, true, true)).isInstanceOf(InvalidSettingsException.class);

		assertThat(store.current()).isEqualTo(new SystemSettings(72, false, true));
		assertThat(store.updatedBy).isNull();
	}

	@Test
	void settingsRejectNonPositiveHoursAsAnInvariant() {
		assertThatThrownBy(() -> new SystemSettings(0, false, false)).isInstanceOf(IllegalArgumentException.class);
	}

	private static final class InMemorySettingsStore implements SystemSettingsStore {

		private SystemSettings settings = new SystemSettings(72, false, true);
		private Long updatedBy;

		@Override
		public SystemSettings current() {
			return settings;
		}

		@Override
		public void save(SystemSettings settings, long updatedBy) {
			this.settings = settings;
			this.updatedBy = updatedBy;
		}

	}

}
