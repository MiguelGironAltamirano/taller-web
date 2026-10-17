package pe.edu.unmsm.servicios.notifications.application;

import java.time.Instant;
import java.util.List;
import pe.edu.unmsm.servicios.notifications.domain.Notification;

public interface NotificationRepository {

	void saveAll(List<Long> recipientIds, long requestId, String text, Instant createdAt);

	List<Notification> findByRecipient(long recipientId);

	boolean markAsRead(long notificationId, long recipientId, Instant readAt);

	int markAllAsRead(long recipientId, Instant readAt);

}
