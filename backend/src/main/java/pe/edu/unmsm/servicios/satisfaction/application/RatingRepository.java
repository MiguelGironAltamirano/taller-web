package pe.edu.unmsm.servicios.satisfaction.application;

import pe.edu.unmsm.servicios.satisfaction.domain.Rating;

/** Storage of the ratings; only this module writes them. */
public interface RatingRepository extends RatingQueries {

	void save(Rating rating);

}
