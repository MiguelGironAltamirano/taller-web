package pe.edu.unmsm.servicios.notifications.application;

import java.time.Clock;
import java.util.List;
import pe.edu.unmsm.servicios.notifications.domain.Notification;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class NotificationUseCases implements NotificationService {

	private final NotificationRepository repository;
	private final Clock clock;

	public NotificationUseCases(NotificationRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Override
	public void notify(List<Long> recipientIds, String text, long requestId) {
		if (recipientIds == null || recipientIds.isEmpty()) {
			return;
		}
		repository.saveAll(recipientIds, requestId, text, clock.instant());
	}

	public List<Notification> listNotifications(AuthenticatedUser user) {
		return repository.findByRecipient(user.id());
	}

	public void markRead(AuthenticatedUser user, long notificationId) {
		boolean updated = repository.markAsRead(notificationId, user.id(), clock.instant());
		if (!updated) {
			throw new NotificationNotFoundException(notificationId);
		}
	}

	public void markAllRead(AuthenticatedUser user) {
		repository.markAllAsRead(user.id(), clock.instant());
	}

}
