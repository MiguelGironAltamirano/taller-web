package pe.edu.unmsm.servicios.notifications.application;

import java.util.List;

public interface NotificationService {

	void notify(List<Long> recipientIds, String text, long requestId);

}
