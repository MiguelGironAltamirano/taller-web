package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.satisfaction.application.AutoCloseResolvedRequests;

/** Runs the auto-close every 15 minutes by default; the tests disable it with {@code app.satisfaction.auto-close-cron=-}. */
@Component
class AutoCloseScheduler {

	private static final Logger log = LoggerFactory.getLogger(AutoCloseScheduler.class);

	private final AutoCloseResolvedRequests autoClose;

	AutoCloseScheduler(AutoCloseResolvedRequests autoClose) {
		this.autoClose = autoClose;
	}

	@Scheduled(cron = "${app.satisfaction.auto-close-cron:0 */15 * * * *}")
	void run() {
		int closed = autoClose.run();
		if (closed > 0) {
			log.info("Auto-closed {} resolved requests without an answer", closed);
		}
	}

}
