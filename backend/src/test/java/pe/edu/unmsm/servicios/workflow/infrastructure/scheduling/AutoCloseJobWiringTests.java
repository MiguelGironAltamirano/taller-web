package pe.edu.unmsm.servicios.workflow.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import pe.edu.unmsm.servicios.requests.application.UnconfirmedResolutions;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;
import pe.edu.unmsm.servicios.workflow.application.AutoCloseUnconfirmedRequests;

/**
 * The job's switch and delays, without a database: the use case reads an empty list, so a run (if any) does
 * nothing. The real closure is covered by AutoCloseApiTests.
 */
class AutoCloseJobWiringTests {

	@Configuration
	@EnableScheduling
	static class UseCase {

		@Bean
		AutoCloseUnconfirmedRequests autoCloseUnconfirmedRequests() {
			UnconfirmedResolutions none = new UnconfirmedResolutions() {

				@Override
				public List<String> resolvedAtOrBefore(Instant cutoff, int limit) {
					return List.of();
				}

				@Override
				public Optional<Instant> resolvedAt(String code) {
					return Optional.empty();
				}

			};
			return new AutoCloseUnconfirmedRequests(none, null, () -> new SystemSettings(72, false, true),
					Clock.systemUTC(), 10);
		}

	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(UseCase.class, AutoCloseJob.class);

	@Test
	void theJobRunsByDefaultWithTheConfiguredDelays() {
		runner.withPropertyValues("app.workflow.auto-close.interval=PT2H", "app.workflow.auto-close.initial-delay=PT3H")
				.run(context -> {
					assertThat(context).hasSingleBean(AutoCloseJob.class);
					assertThat(context.getBean(ScheduledTaskHolder.class).getScheduledTasks()).anySatisfy(
							scheduled -> assertThat(scheduled.getTask()).isInstanceOfSatisfying(FixedDelayTask.class,
									task -> {
										assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofHours(2));
										assertThat(task.getInitialDelayDuration()).isEqualTo(Duration.ofHours(3));
									}));
				});
	}

	@Test
	void theJobCanBeSwitchedOff() {
		runner.withPropertyValues("app.workflow.auto-close.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(AutoCloseJob.class));
	}

}
