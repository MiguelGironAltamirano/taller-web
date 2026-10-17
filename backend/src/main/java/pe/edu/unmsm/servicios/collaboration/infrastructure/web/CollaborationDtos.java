package pe.edu.unmsm.servicios.collaboration.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import pe.edu.unmsm.servicios.collaboration.domain.Comment;

final class CollaborationDtos {

	private CollaborationDtos() {
	}

	record CreateCommentRequest(
			@NotBlank(message = "El texto del comentario es obligatorio")
			@Size(min = 1, max = 1000, message = "El comentario no puede superar los 1000 caracteres") String text) {
	}

	record CommentResponse(long id, String authorName, String text, String createdAt) {
		static CommentResponse from(Comment c) {
			return new CommentResponse(c.id(), c.authorName(), c.text(), c.createdAt().toString());
		}
	}

}
