package pe.edu.unmsm.servicios.satisfaction.domain;

/** The request cannot be rated now: it was not closed by confirmation or it is already rated (409). */
public class RatingNotAllowedException extends RuntimeException {

	public RatingNotAllowedException(String message) {
		super(message);
	}

}
