package pe.edu.unmsm.servicios.collaboration.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

	@Test
	void validCommentCreatedSuccessfully() {
		Instant now = Instant.now();
		Comment comment = Comment.create(10L, 20L, "Juan Pérez", "  Problema resuelto en piso 3.  ", now);

		assertThat(comment.requestId()).isEqualTo(10L);
		assertThat(comment.authorId()).isEqualTo(20L);
		assertThat(comment.authorName()).isEqualTo("Juan Pérez");
		assertThat(comment.text()).isEqualTo("Problema resuelto en piso 3.");
		assertThat(comment.createdAt()).isEqualTo(now);
	}

	@Test
	void emptyOrBlankCommentThrowsCommentTextInvalidException() {
		Instant now = Instant.now();
		assertThatThrownBy(() -> Comment.create(10L, 20L, "Juan Pérez", "", now))
				.isInstanceOf(CommentTextInvalidException.class);

		assertThatThrownBy(() -> Comment.create(10L, 20L, "Juan Pérez", "    ", now))
				.isInstanceOf(CommentTextInvalidException.class);

		assertThatThrownBy(() -> Comment.create(10L, 20L, "Juan Pérez", null, now))
				.isInstanceOf(CommentTextInvalidException.class);
	}

	@Test
	void commentOver1000CharsThrowsCommentTextInvalidException() {
		Instant now = Instant.now();
		String over1000 = "a".repeat(1001);
		assertThatThrownBy(() -> Comment.create(10L, 20L, "Juan Pérez", over1000, now))
				.isInstanceOf(CommentTextInvalidException.class);
	}

	@Test
	void commentAt1000CharsIsAccepted() {
		Instant now = Instant.now();
		String exact1000 = "a".repeat(1000);
		Comment comment = Comment.create(10L, 20L, "Juan Pérez", exact1000, now);
		assertThat(comment.text()).hasSize(1000);
	}

}
