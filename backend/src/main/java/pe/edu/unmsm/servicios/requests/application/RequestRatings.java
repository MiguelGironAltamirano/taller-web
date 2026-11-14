package pe.edu.unmsm.servicios.requests.application;

import java.util.Optional;

/**
 * The satisfaction rating of a request, shown in its detail. The satisfaction module owns ratings and provides
 * the implementation, so the dependency goes from satisfaction to requests.
 */
public interface RequestRatings {

	Optional<RatingView> forRequest(long requestId);

}
