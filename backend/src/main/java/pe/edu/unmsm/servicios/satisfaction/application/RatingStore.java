package pe.edu.unmsm.servicios.satisfaction.application;

import pe.edu.unmsm.servicios.satisfaction.domain.Rating;

public interface RatingStore {

	Rating save(Rating rating);

	boolean existsByRequestId(long requestId);

}
