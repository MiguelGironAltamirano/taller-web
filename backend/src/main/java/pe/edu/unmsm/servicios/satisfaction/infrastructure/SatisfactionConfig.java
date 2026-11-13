package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.satisfaction.application.AutoCloseResolvedRequests;
import pe.edu.unmsm.servicios.satisfaction.application.RateRequest;
import pe.edu.unmsm.servicios.satisfaction.application.RatingStore;
import pe.edu.unmsm.servicios.satisfaction.application.ResolvedRequestStore;

@Configuration
public class SatisfactionConfig {

	@Bean
	public RateRequest rateRequest(RequestStateStore requests, RatingStore ratings, Clock clock) {
		return new RateRequest(requests, ratings, clock);
	}

	@Bean
	public AutoCloseResolvedRequests autoCloseResolvedRequests(ResolvedRequestStore store, Clock clock) {
		return new AutoCloseResolvedRequests(store, clock);
	}

}
