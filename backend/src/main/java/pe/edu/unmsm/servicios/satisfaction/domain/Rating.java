package pe.edu.unmsm.servicios.satisfaction.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The satisfaction rating a requester gives to a request after confirming its solution (spec §6): a score from 1
 * to 5 and an optional comment. Rating is optional and does not change the status of the request.
 */
public record Rating(long requestId, long requesterId, int score, String comment, Instant createdAt) {

	public static final int SCORE_MIN = 1;
	public static final int SCORE_MAX = 5;
	public static final int COMMENT_MAX = 500;

	/** Validates the score and the comment; a blank comment is the same as no comment. */
	public static Rating give(long requestId, long requesterId, Integer score, String comment, Instant now) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (score == null || score < SCORE_MIN || score > SCORE_MAX) {
			errors.put("score", "Califica de " + SCORE_MIN + " a " + SCORE_MAX + " estrellas");
		}
		String text = comment == null || comment.isBlank() ? null : comment.strip();
		if (text != null && text.length() > COMMENT_MAX) {
			errors.put("comment", "El comentario no puede superar los " + COMMENT_MAX + " caracteres");
		}
		if (!errors.isEmpty()) {
			throw new InvalidRatingException(errors);
		}
		return new Rating(requestId, requesterId, score, text, now);
	}

}
