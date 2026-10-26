package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.requests.application.RequestRatings;
import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.satisfaction.application.RateRequest;
import pe.edu.unmsm.servicios.satisfaction.application.RatingQueries;
import pe.edu.unmsm.servicios.satisfaction.application.RatingRepository;

@Configuration
class SatisfactionConfig {

	@Bean
	RateRequest rateRequest(RequestStateStore requests, RatingRepository ratings, Clock clock) {
		return new RateRequest(requests, ratings, clock);
	}

	/** The request detail shows the rating; the requests module only knows its own port. */
	@Bean
	RequestRatings requestRatings(RatingQueries ratings) {
		return new RatingsForRequestDetail(ratings);
	}

}
