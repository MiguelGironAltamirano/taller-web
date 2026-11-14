package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import pe.edu.unmsm.servicios.notifications.application.NotificationService;
import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.satisfaction.application.AutoCloseResolvedRequests;
import pe.edu.unmsm.servicios.satisfaction.application.RateRequest;
import pe.edu.unmsm.servicios.satisfaction.application.RatingStore;
import pe.edu.unmsm.servicios.satisfaction.application.ResolvedRequestStore;

@Configuration
@EnableScheduling
class SatisfactionConfig {

	@Bean
	RateRequest rateRequest(RequestStateStore requests, RatingStore ratings, Clock clock) {
		return new RateRequest(requests, ratings, clock);
	}

	@Bean
	AutoCloseResolvedRequests autoCloseResolvedRequests(ResolvedRequestStore store,
			NotificationService notifications, Clock clock) {
		return new AutoCloseResolvedRequests(store, notifications, clock);
	}

}
