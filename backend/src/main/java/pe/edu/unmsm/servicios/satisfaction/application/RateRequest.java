package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingAlreadyExistsException;
import pe.edu.unmsm.servicios.satisfaction.domain.RequestNotResolvedException;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class RateRequest {

	private final RequestStateStore requests;
	private final RatingStore ratings;
	private final Clock clock;

	public RateRequest(RequestStateStore requests, RatingStore ratings, Clock clock) {
		this.requests = requests;
		this.ratings = ratings;
		this.clock = clock;
	}

	@Transactional
	public Rating execute(AuthenticatedUser actor, String code, int score, String comment) {
		ServiceRequest request = requests.findByCodeForUpdate(code)
				.filter(r -> AccessPolicy.isOwner(actor, r.requesterId()))
				.orElseThrow(() -> new RequestNotFoundException(code));

		if (request.status() != RequestStatus.RESUELTA) {
			throw new RequestNotResolvedException(code);
		}
		if (ratings.existsByRequestId(request.id())) {
			throw new RatingAlreadyExistsException(code);
		}

		return ratings.save(Rating.of(request.id(), score, comment, clock.instant()));
	}

}
