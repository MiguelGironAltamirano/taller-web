package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingAlreadyExistsException;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingNotAllowedException;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * Saves the optional rating of a request whose solution the requester already confirmed. Confirming is the
 * Resuelta → Cerrada transition of the workflow module; rating does not change the status (spec §6).
 */
public class RateRequest {

	private final RequestStateStore requests;
	private final RatingStore ratings;
	private final Clock clock;

	public RateRequest(RequestStateStore requests, RatingStore ratings, Clock clock) {
		this.requests = requests;
		this.ratings = ratings;
		this.clock = clock;
	}

	/** The request row stays locked until the end, so two ratings of one request are decided one after the other. */
	@Transactional
	public Rating execute(AuthenticatedUser actor, String code, int score, String comment) {
		ServiceRequest request = requests.findByCodeForUpdate(code)
				.filter(r -> AccessPolicy.isOwner(actor, r.requesterId()))
				.orElseThrow(() -> new RequestNotFoundException(code));

		if (request.status() != RequestStatus.CERRADA || request.closureReason() != ClosureReason.CONFIRMADA) {
			throw new RatingNotAllowedException(code);
		}
		if (ratings.existsForRequest(request.id())) {
			throw new RatingAlreadyExistsException(code);
		}
		return ratings.save(Rating.of(request.id(), score, comment, clock.instant()));
	}

}
