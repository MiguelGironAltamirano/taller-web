package pe.edu.unmsm.servicios.collaboration.domain;

import java.time.Instant;

public record Comment(Long id, long requestId, long authorId, String authorName, String text, Instant createdAt) {

	public static Comment create(long requestId, long authorId, String authorName, String text, Instant createdAt) {
		if (text == null || text.trim().isEmpty()) {
			throw new CommentTextInvalidException("El comentario no puede estar vacío");
		}
		String trimmed = text.trim();
		if (trimmed.length() > 1000) {
			throw new CommentTextInvalidException("El comentario no puede superar los 1000 caracteres");
		}
		return new Comment(null, requestId, authorId, authorName, trimmed, createdAt);
	}

}
