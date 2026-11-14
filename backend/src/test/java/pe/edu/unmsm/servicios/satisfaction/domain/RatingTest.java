package pe.edu.unmsm.servicios.satisfaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RatingTest {

	private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

	@ParameterizedTest
	@ValueSource(ints = { 1, 3, 5 })
	void acceptsScoresFromOneToFive(int score) {
		assertThat(Rating.of(1, score, null, NOW).score()).isEqualTo(score);
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, 0, 6 })
	void rejectsScoresOutsideOneToFive(int score) {
		assertThatThrownBy(() -> Rating.of(1, score, null, NOW))
				.isInstanceOfSatisfying(InvalidRatingException.class, e -> assertThat(e.field()).isEqualTo("score"));
	}

	@Test
	void blankCommentIsStoredAsNoComment() {
		assertThat(Rating.of(1, 4, "   ", NOW).comment()).isNull();
	}

	@Test
	void commentIsTrimmed() {
		assertThat(Rating.of(1, 4, "  Muy rápido  ", NOW).comment()).isEqualTo("Muy rápido");
	}

	@Test
	void commentLongerThanTheLimitIsRejected() {
		assertThatThrownBy(() -> Rating.of(1, 4, "a".repeat(Rating.COMMENT_MAX + 1), NOW))
				.isInstanceOfSatisfying(InvalidRatingException.class, e -> assertThat(e.field()).isEqualTo("comment"));
	}

}
