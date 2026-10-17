package pe.edu.unmsm.servicios.notifications.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.notifications.application.NotificationRepository;
import pe.edu.unmsm.servicios.notifications.application.NotificationService;
import pe.edu.unmsm.servicios.notifications.application.NotificationUseCases;

@Configuration
public class NotificationsConfig {

	@Bean
	public NotificationUseCases notificationUseCases(NotificationRepository repository, Clock clock) {
		return new NotificationUseCases(repository, clock);
	}

	@Bean
	public NotificationService notificationService(NotificationUseCases useCases) {
		return useCases;
	}

}
