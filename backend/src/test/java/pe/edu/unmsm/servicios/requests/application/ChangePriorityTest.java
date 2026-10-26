package pe.edu.unmsm.servicios.requests.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.Catalogs;
import pe.edu.unmsm.servicios.catalog.domain.Category;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;
import pe.edu.unmsm.servicios.requests.domain.InvalidRequestException;
import pe.edu.unmsm.servicios.requests.domain.RequestChangeForbiddenException;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestNotModifiableException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

class ChangePriorityTest {

	private static final Instant CREATED = Instant.parse("2026-10-16T10:00:00Z");
	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");
	private static final String CODE = "SOL-2026-000001";

	private static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser(2, "s@unmsm.edu.pe", "Sup", "SUPERVISOR",
			1L);
	private static final AuthenticatedUser OTHER_SUPERVISOR = new AuthenticatedUser(6, "s2@unmsm.edu.pe", "Sup2",
			"SUPERVISOR", 3L);
	private static final AuthenticatedUser TECHNICIAN = new AuthenticatedUser(3, "t@unmsm.edu.pe", "Tec", "TECNICO", 1L);
	private static final AuthenticatedUser OWNER = new AuthenticatedUser(4, "o@unmsm.edu.pe", "Owner", "SOLICITANTE");

	private final Map<String, ServiceRequest> stored = new HashMap<>();
	private final List<RequestHistoryEntry> history = new ArrayList<>();

	private final RequestStateStore store = new RequestStateStore() {

		@Override
		public Optional<ServiceRequest> findByCodeForUpdate(String code) {
			return Optional.ofNullable(stored.get(code));
		}

		@Override
		public ServiceRequest update(ServiceRequest request, RequestHistoryEntry entry) {
			stored.put(request.code(), request);
			history.add(entry);
			return request;
		}

	};

	private static final Map<Long, PriorityLevel> PRIORITIES = Map.of(1L, new PriorityLevel(1, "Alta", 8), 2L,
			new PriorityLevel(2, "Media", 24));

	private final CatalogLookup catalog = new CatalogLookup() {

		@Override
		public Catalogs activeCatalogs() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Category> findActiveCategory(long id) {
			return Optional.empty();
		}

		@Override
		public Optional<PriorityLevel> findPriority(long id) {
			return Optional.ofNullable(PRIORITIES.get(id));
		}

		@Override
		public Optional<Location> findLocation(long id) {
			return Optional.empty();
		}

	};

	private final ChangePriority useCase = new ChangePriority(store, catalog, Clock.fixed(NOW, ZoneOffset.UTC));

	private void request(RequestStatus status) {
		stored.put(CODE, new ServiceRequest(10L, CODE, 4, 1, 3, "Descripción de la solicitud", 2, status, 1,
				status == RequestStatus.REGISTRADA ? null : 3L, null, null, CREATED, CREATED.plus(Duration.ofHours(24)),
				CREATED));
	}

	@Test
	void theSupervisorChangesThePriorityAndTheDueDateFollowsTheNewSla() {
		request(RequestStatus.ASIGNADA);

		ServiceRequest changed = useCase.execute(SUPERVISOR, CODE, 1L);

		assertThat(changed.priorityId()).isEqualTo(1L);
		assertThat(changed.dueAt()).isEqualTo(CREATED.plus(Duration.ofHours(8)));
		assertThat(changed.status()).isEqualTo(RequestStatus.ASIGNADA);
		assertThat(history).singleElement().satisfies(entry -> {
			assertThat(entry.fromStatus()).isEqualTo(RequestStatus.ASIGNADA);
			assertThat(entry.toStatus()).isEqualTo(RequestStatus.ASIGNADA);
			assertThat(entry.actorId()).isEqualTo(2);
			assertThat(entry.note()).isEqualTo("Prioridad Media → Prioridad Alta");
			// 18:00 UTC is 13:00 in Lima.
			assertThat(entry.reason()).isEqualTo("Nuevo vencimiento: 16/10/2026 13:00");
			assertThat(entry.createdAt()).isEqualTo(NOW);
		});
	}

	@Test
	void anInvisibleOrUnknownRequestIsNotFound() {
		request(RequestStatus.REGISTRADA);

		assertThatThrownBy(() -> useCase.execute(OTHER_SUPERVISOR, CODE, 1L))
				.isInstanceOf(RequestNotFoundException.class);
		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, "SOL-2026-999999", 1L))
				.isInstanceOf(RequestNotFoundException.class);
		assertThat(history).isEmpty();
	}

	@Test
	void onlyTheSupervisorOfTheAreaMayChangeIt() {
		request(RequestStatus.ASIGNADA);

		assertThatThrownBy(() -> useCase.execute(TECHNICIAN, CODE, 1L))
				.isInstanceOf(RequestChangeForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(OWNER, CODE, 1L)).isInstanceOf(RequestChangeForbiddenException.class);
		assertThat(history).isEmpty();
	}

	@Test
	void aResolvedOrClosedRequestIsAConflictEvenWithAnInvalidPriority() {
		for (RequestStatus status : List.of(RequestStatus.RESUELTA, RequestStatus.CERRADA)) {
			request(status);
			assertThatThrownBy(() -> useCase.execute(SUPERVISOR, CODE, 99L)).as(status.name())
					.isInstanceOf(RequestNotModifiableException.class);
		}
		assertThat(history).isEmpty();
	}

	@Test
	void anUnknownOrUnchangedPriorityIsInvalid() {
		request(RequestStatus.REGISTRADA);

		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, CODE, 99L)).isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, CODE, 2L)).isInstanceOf(InvalidRequestException.class);
		assertThat(history).isEmpty();
	}

	@Test
	void theDetailOffersThePriorityChangeOnlyToTheSupervisorWhileItCanChange() {
		assertThat(ChangePriority.mayChange(SUPERVISOR, 1, RequestStatus.EN_ATENCION)).isTrue();
		assertThat(ChangePriority.mayChange(SUPERVISOR, 1, RequestStatus.RESUELTA)).isFalse();
		assertThat(ChangePriority.mayChange(OTHER_SUPERVISOR, 1, RequestStatus.REGISTRADA)).isFalse();
		assertThat(ChangePriority.mayChange(TECHNICIAN, 1, RequestStatus.ASIGNADA)).isFalse();
	}

}
