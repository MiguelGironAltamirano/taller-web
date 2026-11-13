package pe.edu.unmsm.servicios.satisfaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingAlreadyExistsException;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingNotAllowedException;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

class RateRequestTest {

	private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");
	private static final String CODE = "SOL-2026-000001";
	private static final long REQUESTER_ID = 4;
	private static final AuthenticatedUser OWNER = new AuthenticatedUser(REQUESTER_ID, "a@unmsm.edu.pe", "Owner",
			"SOLICITANTE");
	private static final AuthenticatedUser OTHER = new AuthenticatedUser(5, "b@unmsm.edu.pe", "Other", "SOLICITANTE");

	private final FakeRatings ratings = new FakeRatings();

	private RateRequest useCase(RequestStatus status, ClosureReason closure) {
		ServiceRequest request = new ServiceRequest(10L, CODE, REQUESTER_ID, 1, 3, "Descripción de prueba suficiente",
				2, status, 1, 3L, closure, NOW, NOW, NOW);
		RequestStateStore store = new RequestStateStore() {
			@Override
			public Optional<ServiceRequest> findByCodeForUpdate(String code) {
				return CODE.equals(code) ? Optional.of(request) : Optional.empty();
			}

			@Override
			public ServiceRequest update(ServiceRequest r, RequestHistoryEntry entry) {
				throw new AssertionError("Rating must not change the request");
			}
		};
		return new RateRequest(store, ratings, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void ownerRatesARequestWhoseSolutionTheyConfirmed() {
		Rating rating = useCase(RequestStatus.CERRADA, ClosureReason.CONFIRMADA).execute(OWNER, CODE, 4, " Bien ");

		assertThat(ratings.saved).singleElement().satisfies(saved -> {
			assertThat(saved.requestId()).isEqualTo(10L);
			assertThat(saved.score()).isEqualTo(4);
			assertThat(saved.comment()).isEqualTo("Bien");
			assertThat(saved.createdAt()).isEqualTo(NOW);
		});
		assertThat(rating.id()).isNotNull();
	}

	@Test
	void anotherUsersRequestIsReportedAsNotFound() {
		assertThatThrownBy(() -> useCase(RequestStatus.CERRADA, ClosureReason.CONFIRMADA).execute(OTHER, CODE, 4, null))
				.isInstanceOf(RequestNotFoundException.class);
		assertThat(ratings.saved).isEmpty();
	}

	@Test
	void unknownCodeIsReportedAsNotFound() {
		assertThatThrownBy(
				() -> useCase(RequestStatus.CERRADA, ClosureReason.CONFIRMADA).execute(OWNER, "SOL-2026-999999", 4, null))
				.isInstanceOf(RequestNotFoundException.class);
	}

	@Test
	void resolvedButNotConfirmedCannotBeRated() {
		assertThatThrownBy(() -> useCase(RequestStatus.RESUELTA, null).execute(OWNER, CODE, 4, null))
				.isInstanceOf(RatingNotAllowedException.class);
	}

	@Test
	void requestsClosedWithoutTheRequestersConfirmationCannotBeRated() {
		for (ClosureReason closure : List.of(ClosureReason.NO_PROCEDE, ClosureReason.DUPLICADA,
				ClosureReason.AUTOMATICA)) {
			assertThatThrownBy(() -> useCase(RequestStatus.CERRADA, closure).execute(OWNER, CODE, 4, null))
					.as(closure.name())
					.isInstanceOf(RatingNotAllowedException.class);
		}
		assertThat(ratings.saved).isEmpty();
	}

	@Test
	void aRequestIsRatedOnlyOnce() {
		RateRequest rate = useCase(RequestStatus.CERRADA, ClosureReason.CONFIRMADA);
		rate.execute(OWNER, CODE, 5, null);

		assertThatThrownBy(() -> rate.execute(OWNER, CODE, 3, null)).isInstanceOf(RatingAlreadyExistsException.class);
		assertThat(ratings.saved).hasSize(1);
	}

	private static final class FakeRatings implements RatingStore {

		private final List<Rating> saved = new ArrayList<>();

		@Override
		public Rating save(Rating rating) {
			Rating stored = new Rating((long) saved.size() + 1, rating.requestId(), rating.score(), rating.comment(),
					rating.createdAt());
			saved.add(stored);
			return stored;
		}

		@Override
		public boolean existsForRequest(long requestId) {
			return saved.stream().anyMatch(r -> r.requestId() == requestId);
		}

	}

}
