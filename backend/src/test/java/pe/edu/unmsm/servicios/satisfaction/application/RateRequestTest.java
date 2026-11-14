package pe.edu.unmsm.servicios.satisfaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestChangeForbiddenException;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.satisfaction.domain.InvalidRatingException;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingNotAllowedException;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

class RateRequestTest {

	private static final Instant CREATED = Instant.parse("2026-10-16T10:00:00Z");
	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
	private static final String CODE = "SOL-2026-000001";

	private static final AuthenticatedUser OWNER = new AuthenticatedUser(4, "o@unmsm.edu.pe", "Owner", "SOLICITANTE");
	private static final AuthenticatedUser OTHER_REQUESTER = new AuthenticatedUser(5, "r@unmsm.edu.pe", "Other",
			"SOLICITANTE");
	private static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser(2, "s@unmsm.edu.pe", "Sup", "SUPERVISOR",
			1L);

	private final Map<String, ServiceRequest> requests = new HashMap<>();
	private final Map<Long, Rating> ratings = new HashMap<>();
	private final List<Rating> saved = new ArrayList<>();

	private final RequestStateStore store = new RequestStateStore() {

		@Override
		public Optional<ServiceRequest> findByCodeForUpdate(String code) {
			return Optional.ofNullable(requests.get(code));
		}

		@Override
		public ServiceRequest update(ServiceRequest request, RequestHistoryEntry entry) {
			throw new AssertionError("rating never changes the request");
		}

	};

	private final RatingRepository repository = new RatingRepository() {

		@Override
		public void save(Rating rating) {
			saved.add(rating);
			ratings.put(rating.requestId(), rating);
		}

		@Override
		public Optional<Rating> findByRequestId(long requestId) {
			return Optional.ofNullable(ratings.get(requestId));
		}

		@Override
		public List<Rating> findCreatedBetween(Instant from, Instant to) {
			return List.copyOf(ratings.values());
		}

	};

	private final RateRequest useCase = new RateRequest(store, repository, Clock.fixed(NOW, ZoneOffset.UTC));

	private void request(RequestStatus status, ClosureReason closure) {
		requests.put(CODE, new ServiceRequest(10L, CODE, 4, 1, 3, "Descripción de la solicitud", 2, status, 1, 3L,
				closure, null, CREATED, CREATED.plusSeconds(86400), CREATED));
	}

	@Test
	void theOwnerRatesARequestClosedByConfirmation() {
		request(RequestStatus.CERRADA, ClosureReason.CONFIRMADA);

		Rating rating = useCase.execute(OWNER, CODE, 4, " Buen trabajo ");

		assertThat(rating.requestId()).isEqualTo(10L);
		assertThat(rating.score()).isEqualTo(4);
		assertThat(rating.comment()).isEqualTo("Buen trabajo");
		assertThat(rating.createdAt()).isEqualTo(NOW);
		assertThat(saved).containsExactly(rating);
	}

	@Test
	void aRequestNotClosedByConfirmationCannotBeRated() {
		for (RequestStatus status : List.of(RequestStatus.REGISTRADA, RequestStatus.EN_ATENCION,
				RequestStatus.RESUELTA)) {
			request(status, null);
			assertThatThrownBy(() -> useCase.execute(OWNER, CODE, 5, null)).as(status.name())
					.isInstanceOf(RatingNotAllowedException.class);
		}
		for (ClosureReason closure : List.of(ClosureReason.NO_PROCEDE, ClosureReason.DUPLICADA,
				ClosureReason.AUTOMATICA)) {
			request(RequestStatus.CERRADA, closure);
			assertThatThrownBy(() -> useCase.execute(OWNER, CODE, 5, null)).as(closure.name())
					.isInstanceOf(RatingNotAllowedException.class);
		}
		assertThat(saved).isEmpty();
	}

	@Test
	void aRequestIsRatedOnlyOnce() {
		request(RequestStatus.CERRADA, ClosureReason.CONFIRMADA);
		useCase.execute(OWNER, CODE, 4, null);

		assertThatThrownBy(() -> useCase.execute(OWNER, CODE, 2, null)).isInstanceOf(RatingNotAllowedException.class);
		assertThat(saved).hasSize(1);
	}

	@Test
	void theConflictIsReportedBeforeInvalidData() {
		request(RequestStatus.RESUELTA, null);

		assertThatThrownBy(() -> useCase.execute(OWNER, CODE, 9, null)).isInstanceOf(RatingNotAllowedException.class);
	}

	@Test
	void anInvalidScoreIsRejected() {
		request(RequestStatus.CERRADA, ClosureReason.CONFIRMADA);

		assertThatThrownBy(() -> useCase.execute(OWNER, CODE, 0, null)).isInstanceOf(InvalidRatingException.class);
		assertThat(saved).isEmpty();
	}

	@Test
	void onlyTheOwnerMayRate() {
		request(RequestStatus.CERRADA, ClosureReason.CONFIRMADA);

		assertThatThrownBy(() -> useCase.execute(OTHER_REQUESTER, CODE, 5, null))
				.isInstanceOf(RequestNotFoundException.class);
		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, CODE, 5, null))
				.isInstanceOf(RequestChangeForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(OWNER, "SOL-2026-999999", 5, null))
				.isInstanceOf(RequestNotFoundException.class);
		assertThat(saved).isEmpty();
	}

}
