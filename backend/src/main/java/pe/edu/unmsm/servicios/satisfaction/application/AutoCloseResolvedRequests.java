package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pe.edu.unmsm.servicios.notifications.application.NotificationService;
import pe.edu.unmsm.servicios.satisfaction.application.ResolvedRequestStore.ResolvedRequest;

/**
 * Closes the requests that stayed in Resuelta for 72 hours without an answer from the requester (spec §6) and tells
 * the requester. Each request is closed in its own transaction, so one failure does not undo or block the others,
 * and a failed notification never undoes a closure.
 */
public class AutoCloseResolvedRequests {

	public static final Duration AUTO_CLOSE_AFTER = Duration.ofHours(72);

	private static final Logger log = LoggerFactory.getLogger(AutoCloseResolvedRequests.class);

	private final ResolvedRequestStore store;
	private final NotificationService notifications;
	private final Clock clock;

	public AutoCloseResolvedRequests(ResolvedRequestStore store, NotificationService notifications, Clock clock) {
		this.store = store;
		this.notifications = notifications;
		this.clock = clock;
	}

	/** Returns how many requests were closed. */
	public int run() {
		Instant now = clock.instant();
		int closed = 0;
		for (ResolvedRequest request : store.findResolvedBefore(now.minus(AUTO_CLOSE_AFTER))) {
			try {
				if (store.closeAutomatically(request.id(), now)) {
					closed++;
					notifyRequester(request);
				}
			}
			catch (RuntimeException e) {
				log.error("Could not auto-close request {}", request.code(), e);
			}
		}
		return closed;
	}

	private void notifyRequester(ResolvedRequest request) {
		try {
			notifications.notify(List.of(request.requesterId()), "La solicitud " + request.code()
					+ " se cerró automáticamente tras 72 horas sin respuesta.", request.id());
		}
		catch (RuntimeException e) {
			log.error("Could not notify the auto-close of request {}", request.code(), e);
		}
	}

}
