package pe.edu.unmsm.servicios.satisfaction.domain;

import java.time.Instant;

public record Rating(Long id, long requestId, int score, String comment, Instant createdAt) {

	public static Rating of(long requestId, int score, String comment, Instant now) {
		if (score < 1 || score > 5) {
			throw new InvalidRatingException("El puntaje debe estar entre 1 y 5");
		}
		return new Rating(null, requestId, score, comment, now);
	}

}
