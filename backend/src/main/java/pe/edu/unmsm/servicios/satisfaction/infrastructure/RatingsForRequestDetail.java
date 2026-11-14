package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import java.util.Optional;

import pe.edu.unmsm.servicios.requests.application.RatingView;
import pe.edu.unmsm.servicios.requests.application.RequestRatings;
import pe.edu.unmsm.servicios.satisfaction.application.RatingQueries;

/** Gives the request detail its rating, read through this module's public read port. */
class RatingsForRequestDetail implements RequestRatings {

	private final RatingQueries ratings;

	RatingsForRequestDetail(RatingQueries ratings) {
		this.ratings = ratings;
	}

	@Override
	public Optional<RatingView> forRequest(long requestId) {
		return ratings.findByRequestId(requestId).map(r -> new RatingView(r.score(), r.comment()));
	}

}
