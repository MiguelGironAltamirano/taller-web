package pe.edu.unmsm.servicios.satisfaction.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AutoCloseResolvedRequestsTest {

	private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void closesAllResolvedRequestsOlderThan72Hours() {
		List<Long> closed = new ArrayList<>();
		ResolvedRequestStore store = new ResolvedRequestStore() {
			@Override
			public List<Long> findResolvedWithoutRatingOlderThan(int hours, Instant now) {
				assertThat(hours).isEqualTo(72);
				return List.of(1L, 2L, 3L);
			}

			@Override
			public void closeWithoutRating(long requestId, Instant now) {
				closed.add(requestId);
			}
		};

		new AutoCloseResolvedRequests(store, clock).run();

		assertThat(closed).containsExactly(1L, 2L, 3L);
	}

	@Test
	void doesNothingWhenNoRequestsToClose() {
		List<Long> closed = new ArrayList<>();
		ResolvedRequestStore store = new ResolvedRequestStore() {
			@Override
			public List<Long> findResolvedWithoutRatingOlderThan(int hours, Instant now) {
				return List.of();
			}

			@Override
			public void closeWithoutRating(long requestId, Instant now) {
				closed.add(requestId);
			}
		};

		new AutoCloseResolvedRequests(store, clock).run();

		assertThat(closed).isEmpty();
	}

}
