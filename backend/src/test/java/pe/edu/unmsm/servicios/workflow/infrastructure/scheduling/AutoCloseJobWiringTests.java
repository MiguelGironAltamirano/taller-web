package pe.edu.unmsm.servicios.workflow.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;

/** With the job enabled (as in production) it is scheduled with the configured delays. It never fires here. */
@SpringBootTest(properties = { "app.workflow.auto-close.enabled=true", "app.workflow.auto-close.interval=PT2H",
		"app.workflow.auto-close.initial-delay=PT3H" })
@Import(TestcontainersConfiguration.class)
class AutoCloseJobWiringTests {

	@Autowired
	AutoCloseJob job;

	@Autowired
	ScheduledTaskHolder tasks;

	@Test
	void theEnabledJobIsScheduledWithTheConfiguredInterval() {
		assertThat(job).isNotNull();
		assertThat(tasks.getScheduledTasks()).anySatisfy(scheduled -> {
			assertThat(scheduled.getTask()).isInstanceOfSatisfying(FixedDelayTask.class, task -> {
				assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofHours(2));
				assertThat(task.getInitialDelayDuration()).isEqualTo(Duration.ofHours(3));
				assertThat(task.getRunnable().toString()).contains("AutoCloseJob.run");
			});
		});
	}

}
