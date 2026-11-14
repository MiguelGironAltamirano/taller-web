package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestChangeForbiddenException;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingNotAllowedException;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * The requester rates a request whose solution they confirmed. The rating is optional, given at most once, and
 * does not change the request: it was already closed by the {@code RESUELTA → CERRADA} transition.
 */
public class RateRequest {

	private static final String REQUESTER = "SOLICITANTE";

	private final RequestStateStore requests;
	private final RatingRepository ratings;
	private final Clock clock;

	public RateRequest(RequestStateStore requests, RatingRepository ratings, Clock clock) {
		this.requests = requests;
		this.ratings = ratings;
		this.clock = clock;
	}

	/**
	 * Checks run in a fixed order: not visible (404), not the owning requester (403), not closed by confirmation
	 * or already rated (409), invalid score or comment (400). The request row stays locked until the rating
	 * commits, so two simultaneous ratings are decided one after the other.
	 */
	@Transactional
	public Rating execute(AuthenticatedUser user, String code, Integer score, String comment) {
		ServiceRequest request = requests.findByCodeForUpdate(code)
				.filter(r -> AccessPolicy.canViewRequest(user, r.requesterId(), r.areaId(), r.technicianId()))
				.orElseThrow(() -> new RequestNotFoundException(code));
		if (!REQUESTER.equals(user.role()) || !AccessPolicy.isOwner(user, request.requesterId())) {
			throw new RequestChangeForbiddenException("Solo el solicitante puede calificar su solicitud");
		}
		if (request.status() != RequestStatus.CERRADA || request.closureReason() != ClosureReason.CONFIRMADA) {
			throw new RatingNotAllowedException("Solo se puede calificar una solicitud cuya solución confirmaste");
		}
		if (ratings.findByRequestId(request.id()).isPresent()) {
			throw new RatingNotAllowedException("Ya calificaste esta solicitud");
		}
		Rating rating = Rating.give(request.id(), score, comment, clock.instant());
		ratings.save(rating);
		return rating;
	}

}
