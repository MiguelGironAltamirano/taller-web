package pe.edu.unmsm.servicios.collaboration.infrastructure.adapter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.collaboration.application.CommentNotifier;
import pe.edu.unmsm.servicios.notifications.application.NotificationService;

@Component
public class DefaultCommentNotifier implements CommentNotifier {

	private static final Logger log = LoggerFactory.getLogger(DefaultCommentNotifier.class);

	private final NotificationService notificationService;

	public DefaultCommentNotifier(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
	public void notifyComment(String requestCode, long requestId, List<Long> recipientIds, String text) {
		try {
			log.info("Notifying comment on {} (id: {}) to {}: {}", requestCode, requestId, recipientIds, text);
			notificationService.notify(recipientIds, text, requestId);
		} catch (Exception e) {
			log.error("Failed to notify comment on {}: {}", requestCode, e.getMessage(), e);
		}
	}

}
