package pe.edu.unmsm.servicios.workflow.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import pe.edu.unmsm.servicios.requests.application.UnconfirmedResolutions;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;
import pe.edu.unmsm.servicios.workflow.application.AutoCloseUnconfirmedRequests;

class AutoCloseJobTests {

	@Test
	void runsWithAConfigurableFixedDelay() throws Exception {
		Scheduled scheduled = AutoCloseJob.class.getDeclaredMethod("run").getAnnotation(Scheduled.class);

		assertThat(scheduled.fixedDelayString()).isEqualTo("${app.workflow.auto-close.interval:PT15M}");
		assertThat(scheduled.initialDelayString()).isEqualTo("${app.workflow.auto-close.initial-delay:PT1M}");
	}

	@Test
	void aFailedRunDoesNotStopTheSchedule() {
		UnconfirmedResolutions broken = new UnconfirmedResolutions() {

			@Override
			public List<String> resolvedAtOrBefore(Instant cutoff, int limit) {
				throw new IllegalStateException("database unavailable");
			}

			@Override
			public Optional<Instant> resolvedAt(String code) {
				return Optional.empty();
			}

		};
		AutoCloseJob job = new AutoCloseJob(new AutoCloseUnconfirmedRequests(broken, null,
				() -> new SystemSettings(72, false, true), Clock.systemUTC(), 10));

		assertThatCode(job::run).doesNotThrowAnyException();
	}

}
