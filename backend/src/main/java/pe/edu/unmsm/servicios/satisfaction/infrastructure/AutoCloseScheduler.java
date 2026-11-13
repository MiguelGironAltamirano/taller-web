package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.satisfaction.application.AutoCloseResolvedRequests;

/** Triggers the auto-close use case on a schedule. Keeps Spring annotations out of the application layer. */
@Component
class AutoCloseScheduler {

	private final AutoCloseResolvedRequests autoClose;

	AutoCloseScheduler(AutoCloseResolvedRequests autoClose) {
		this.autoClose = autoClose;
	}

	@Scheduled(cron = "${satisfaction.auto-close.cron:0 0 * * * *}")
	@Transactional
	public void run() {
		autoClose.run();
	}

}
