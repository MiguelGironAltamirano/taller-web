package pe.edu.unmsm.servicios.satisfaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RatingTest {

	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

	@ParameterizedTest
	@ValueSource(ints = { 1, 3, 5 })
	void scoresFromOneToFiveAreAccepted(int score) {
		Rating rating = Rating.give(10, 4, score, null, NOW);

		assertThat(rating.score()).isEqualTo(score);
		assertThat(rating.requestId()).isEqualTo(10);
		assertThat(rating.requesterId()).isEqualTo(4);
		assertThat(rating.createdAt()).isEqualTo(NOW);
	}

	@Test
	void scoresOutsideTheRangeOrMissingAreRejected() {
		for (Integer score : new Integer[] { 0, 6, -1, null }) {
			assertThatThrownBy(() -> Rating.give(10, 4, score, null, NOW)).as(String.valueOf(score))
					.isInstanceOf(InvalidRatingException.class)
					.satisfies(ex -> assertThat(((InvalidRatingException) ex).errors()).containsKey("score"));
		}
	}

	@Test
	void theCommentIsOptionalAndTrimmed() {
		assertThat(Rating.give(10, 4, 5, "  Muy rápido  ", NOW).comment()).isEqualTo("Muy rápido");
		assertThat(Rating.give(10, 4, 5, "   ", NOW).comment()).isNull();
		assertThat(Rating.give(10, 4, 5, null, NOW).comment()).isNull();
	}

	@Test
	void theCommentHasALimit() {
		assertThat(Rating.give(10, 4, 5, "x".repeat(Rating.COMMENT_MAX), NOW).comment()).hasSize(Rating.COMMENT_MAX);
		assertThatThrownBy(() -> Rating.give(10, 4, 5, "x".repeat(Rating.COMMENT_MAX + 1), NOW))
				.isInstanceOf(InvalidRatingException.class)
				.satisfies(ex -> assertThat(((InvalidRatingException) ex).errors()).containsKey("comment"));
	}

}
