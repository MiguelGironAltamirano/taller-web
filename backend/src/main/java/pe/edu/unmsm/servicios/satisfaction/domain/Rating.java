package pe.edu.unmsm.servicios.satisfaction.domain;

import java.time.Instant;

public record Rating(Long id, long requestId, int score, String comment, Instant createdAt) {

	public static final int SCORE_MIN = 1;
	public static final int SCORE_MAX = 5;
	public static final int COMMENT_MAX = 1000;

	/** A blank comment is stored as no comment; spaces around the text do not count. */
	public static Rating of(long requestId, int score, String comment, Instant now) {
		if (score < SCORE_MIN || score > SCORE_MAX) {
			throw new InvalidRatingException("score", "La calificación debe ser de 1 a 5");
		}
		String normalized = comment == null || comment.isBlank() ? null : comment.strip();
		if (normalized != null && normalized.length() > COMMENT_MAX) {
			throw new InvalidRatingException("comment", "El comentario no puede superar los 1000 caracteres");
		}
		return new Rating(null, requestId, score, normalized, now);
	}

}
