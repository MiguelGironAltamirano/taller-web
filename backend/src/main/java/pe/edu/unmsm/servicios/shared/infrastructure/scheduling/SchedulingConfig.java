package pe.edu.unmsm.servicios.shared.infrastructure.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} for the whole application. Each job decides with its own property whether it runs
 * (for example {@code app.workflow.auto-close.enabled}), so tests switch off jobs, not the scheduler.
 */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
