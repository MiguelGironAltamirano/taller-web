package pe.edu.unmsm.servicios.workflow.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pe.edu.unmsm.servicios.requests.application.UnconfirmedResolutions;
import pe.edu.unmsm.servicios.shared.application.SystemSettingsProvider;

/**
 * The automatic closure: requests left in {@code RESUELTA} for at least {@code autoCloseHours} (administrator
 * setting, 72 by default) without the requester confirming or rejecting the solution are closed by the system with
 * {@code ClosureReason.AUTOMATICA}. Each run handles at most one batch; the rest waits for the next run.
 */
public class AutoCloseUnconfirmedRequests {

	private static final Logger log = LoggerFactory.getLogger(AutoCloseUnconfirmedRequests.class);

	private final UnconfirmedResolutions resolutions;
	private final CloseUnconfirmedRequest closer;
	private final SystemSettingsProvider settings;
	private final Clock clock;
	private final int batchSize;

	public AutoCloseUnconfirmedRequests(UnconfirmedResolutions resolutions, CloseUnconfirmedRequest closer,
			SystemSettingsProvider settings, Clock clock, int batchSize) {
		this.resolutions = resolutions;
		this.closer = closer;
		this.settings = settings;
		this.clock = clock;
		this.batchSize = batchSize;
	}

	/** Closes the expired requests and returns how many were closed. */
	public int run() {
		int hours = settings.current().autoCloseHours();
		Instant cutoff = clock.instant().minus(Duration.ofHours(hours));
		int closed = 0;
		for (String code : resolutions.resolvedAtOrBefore(cutoff, batchSize)) {
			try {
				if (closer.execute(code, cutoff, hours).isPresent()) {
					closed++;
				}
			}
			catch (RuntimeException ex) {
				// One failing request must not block the others; it is retried on the next run.
				log.warn("Automatic closure of {} failed: {}", code, ex.getMessage(), ex);
			}
		}
		return closed;
	}

}
