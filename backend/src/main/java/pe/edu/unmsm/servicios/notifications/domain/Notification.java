package pe.edu.unmsm.servicios.notifications.domain;

import java.time.Instant;

public record Notification(
		long id,
		long recipientId,
		long requestId,
		String requestCode,
		String text,
		Instant createdAt,
		boolean read
) {
}
