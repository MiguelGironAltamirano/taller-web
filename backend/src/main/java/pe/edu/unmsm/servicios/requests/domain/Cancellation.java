package pe.edu.unmsm.servicios.requests.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * The last time a request was cancelled: the status it had, to which restoring brings it back, and when. The request
 * may be restored for {@link #RESTORE_WINDOW} (docs/api/contrato-v1.md «Cancelar y restaurar una solicitud»).
 */
public record Cancellation(RequestStatus previousStatus, Instant cancelledAt) {

	public static final Duration RESTORE_WINDOW = Duration.ofDays(7);

	public boolean restorableAt(Instant now) {
		return !now.isAfter(cancelledAt.plus(RESTORE_WINDOW));
	}

}
