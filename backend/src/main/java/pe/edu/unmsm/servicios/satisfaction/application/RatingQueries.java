package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.satisfaction.domain.Rating;

/**
 * Public read port of the satisfaction module, for the modules that show or aggregate ratings (request detail,
 * dashboard). Callers apply their own visibility rules.
 */
public interface RatingQueries {

	Optional<Rating> findByRequestId(long requestId);

	/** Ratings given in {@code [from, to)}, oldest first. */
	List<Rating> findCreatedBetween(Instant from, Instant to);

}
