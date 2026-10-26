package pe.edu.unmsm.servicios.workflow.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.workflow.application.AutoCloseUnconfirmedRequests;

/**
 * Runs the automatic closure periodically. The interval only decides how soon after the deadline a request is
 * closed; the deadline itself ({@code autoCloseHours}) is an administrator setting read on every run. Several
 * instances may run it at the same time: every request is locked and checked again before it is closed.
 *
 * <p>
 * Properties: {@code app.workflow.auto-close.enabled} (default {@code true}), {@code .interval} (fixed delay
 * between runs, ISO-8601, default {@code PT15M}) and {@code .initial-delay} (default {@code PT1M}).
 */
@Component
@ConditionalOnProperty(name = "app.workflow.auto-close.enabled", havingValue = "true", matchIfMissing = true)
class AutoCloseJob {

	private static final Logger log = LoggerFactory.getLogger(AutoCloseJob.class);

	private final AutoCloseUnconfirmedRequests autoClose;

	AutoCloseJob(AutoCloseUnconfirmedRequests autoClose) {
		this.autoClose = autoClose;
	}

	@Scheduled(initialDelayString = "${app.workflow.auto-close.initial-delay:PT1M}",
			fixedDelayString = "${app.workflow.auto-close.interval:PT15M}")
	void run() {
		try {
			int closed = autoClose.run();
			if (closed > 0) {
				log.info("Automatic closure: {} unconfirmed request(s) closed", closed);
			}
		}
		catch (RuntimeException ex) {
			// Keep the schedule alive; the next run tries again.
			log.error("Automatic closure run failed: {}", ex.getMessage(), ex);
		}
	}

}
