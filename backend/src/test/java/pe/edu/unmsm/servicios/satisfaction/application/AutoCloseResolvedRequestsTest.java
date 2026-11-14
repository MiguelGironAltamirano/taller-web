package pe.edu.unmsm.servicios.satisfaction.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.notifications.application.NotificationService;
import pe.edu.unmsm.servicios.satisfaction.application.ResolvedRequestStore.ResolvedRequest;

class AutoCloseResolvedRequestsTest {

	private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

	private final List<String> notified = new ArrayList<>();
	private final NotificationService notifications = (recipients, text, requestId) -> notified
			.add(recipients + " " + text);

	@Test
	void looksForRequestsResolvedAtLeast72HoursAgo() {
		FakeStore store = new FakeStore(List.of(), Set.of(), Set.of());

		new AutoCloseResolvedRequests(store, notifications, clock).run();

		assertThat(store.askedCutoff).isEqualTo(Instant.parse("2026-11-10T12:00:00Z"));
	}

	@Test
	void closesEachRequestAndNotifiesItsRequester() {
		FakeStore store = new FakeStore(List.of(request(1), request(2)), Set.of(), Set.of());

		int closed = new AutoCloseResolvedRequests(store, notifications, clock).run();

		assertThat(closed).isEqualTo(2);
		assertThat(store.closed).containsExactly(1L, 2L);
		assertThat(notified).containsExactly(
				"[41] La solicitud SOL-2026-000001 se cerró automáticamente tras 72 horas sin respuesta.",
				"[42] La solicitud SOL-2026-000002 se cerró automáticamente tras 72 horas sin respuesta.");
	}

	@Test
	void aRequestTheRequesterAnsweredMeanwhileIsNeitherCountedNorNotified() {
		FakeStore store = new FakeStore(List.of(request(1)), Set.of(1L), Set.of());

		int closed = new AutoCloseResolvedRequests(store, notifications, clock).run();

		assertThat(closed).isZero();
		assertThat(notified).isEmpty();
	}

	@Test
	void oneFailureDoesNotStopTheOthers() {
		FakeStore store = new FakeStore(List.of(request(1), request(2)), Set.of(), Set.of(1L));

		int closed = new AutoCloseResolvedRequests(store, notifications, clock).run();

		assertThat(closed).isEqualTo(1);
		assertThat(store.closed).containsExactly(2L);
	}

	@Test
	void aFailedNotificationDoesNotUndoTheClosure() {
		FakeStore store = new FakeStore(List.of(request(1)), Set.of(), Set.of());
		NotificationService failing = (recipients, text, requestId) -> {
			throw new IllegalStateException("notifications down");
		};

		int closed = new AutoCloseResolvedRequests(store, failing, clock).run();

		assertThat(closed).isEqualTo(1);
		assertThat(store.closed).containsExactly(1L);
	}

	private static ResolvedRequest request(long id) {
		return new ResolvedRequest(id, "SOL-2026-00000" + id, 40 + id);
	}

	private static final class FakeStore implements ResolvedRequestStore {

		private final List<ResolvedRequest> candidates;
		private final Set<Long> alreadyAnswered;
		private final Set<Long> failing;
		private final List<Long> closed = new ArrayList<>();
		private Instant askedCutoff;

		FakeStore(List<ResolvedRequest> candidates, Set<Long> alreadyAnswered, Set<Long> failing) {
			this.candidates = candidates;
			this.alreadyAnswered = alreadyAnswered;
			this.failing = failing;
		}

		@Override
		public List<ResolvedRequest> findResolvedBefore(Instant resolvedBefore) {
			askedCutoff = resolvedBefore;
			return candidates;
		}

		@Override
		public boolean closeAutomatically(long requestId, Instant now) {
			if (failing.contains(requestId)) {
				throw new IllegalStateException("database error");
			}
			if (alreadyAnswered.contains(requestId)) {
				return false;
			}
			closed.add(requestId);
			return true;
		}

	}

}
