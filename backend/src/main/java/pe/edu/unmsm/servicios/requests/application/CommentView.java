package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;

/** A comment as listed in the request detail ({@code CommentDto} of the contract). */
public record CommentView(long id, String authorName, String text, Instant createdAt) {
}
