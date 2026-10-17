package pe.edu.unmsm.servicios.workflow.application;

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
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.domain.InvalidTransitionDataException;
import pe.edu.unmsm.servicios.workflow.domain.InvalidTransitionException;
import pe.edu.unmsm.servicios.workflow.domain.Technician;
import pe.edu.unmsm.servicios.workflow.domain.TransitionCommand;
import pe.edu.unmsm.servicios.workflow.domain.TransitionForbiddenException;

class TransitionRequestTest {

	private static final Instant CREATED = Instant.parse("2026-10-16T10:00:00Z");
	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");

	private static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser(2, "s@unmsm.edu.pe", "Sup", "SUPERVISOR",
			1L);
	private static final AuthenticatedUser TECHNICIAN = new AuthenticatedUser(3, "t@unmsm.edu.pe", "Tec", "TECNICO", 1L);
	private static final AuthenticatedUser OWNER = new AuthenticatedUser(4, "o@unmsm.edu.pe", "Owner", "SOLICITANTE");

	private final Map<String, ServiceRequest> stored = new HashMap<>();
	private final List<RequestHistoryEntry> history = new ArrayList<>();
	private int updates;
	private final Map<Long, Technician> technicians = new HashMap<>(Map.of(
			3L, new Technician(3, "Carlos Mendoza", 1L, true),
			9L, new Technician(9, "Rosa Alarcón", 3L, true)));

	private final RequestStateStore store = new RequestStateStore() {

		@Override
		public Optional<ServiceRequest> findByCodeForUpdate(String code) {
			return Optional.ofNullable(stored.get(code));
		}

		@Override
		public ServiceRequest update(ServiceRequest request, RequestHistoryEntry entry) {
			updates++;
			stored.put(request.code(), request);
			history.add(entry);
			return request;
		}

	};

	private final TransitionRequest useCase = new TransitionRequest(store,
			new TechnicianDirectory() {

				@Override
				public Optional<Technician> findTechnician(long id) {
					return Optional.ofNullable(technicians.get(id));
				}

				@Override
				public List<Technician> activeTechniciansOfArea(long areaId) {
					return technicians.values().stream().filter(t -> t.areaId() == areaId).toList();
				}

			}, Clock.fixed(NOW, ZoneOffset.UTC));

	private ServiceRequest request(RequestStatus status, Long technicianId) {
		ServiceRequest request = new ServiceRequest(10L, "SOL-2026-000001", 4, 1, 3, "Descripción de la solicitud",
				2, status, 1, technicianId, null, CREATED, CREATED.plusSeconds(86400), CREATED);
		stored.put(request.code(), request);
		return request;
	}

	@Test
	void anUnknownRequestIsNotFoundAndNothingIsSaved() {
		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, "SOL-2026-999999",
				new TransitionCommand(RequestStatus.EN_EVALUACION, null, null, null)))
				.isInstanceOf(RequestNotFoundException.class);
		assertThat(updates).isZero();
	}

	@Test
	void aValidTransitionUpdatesTheRequestAndAppendsTheHistoryEntry() {
		request(RequestStatus.REGISTRADA, null);

		ServiceRequest result = useCase.execute(SUPERVISOR, "SOL-2026-000001",
				new TransitionCommand(RequestStatus.EN_EVALUACION, null, null, null));

		assertThat(result.status()).isEqualTo(RequestStatus.EN_EVALUACION);
		assertThat(result.updatedAt()).isEqualTo(NOW);
		assertThat(result.createdAt()).as("creation data does not change").isEqualTo(CREATED);
		assertThat(history).singleElement().satisfies(entry -> {
			assertThat(entry.fromStatus()).isEqualTo(RequestStatus.REGISTRADA);
			assertThat(entry.toStatus()).isEqualTo(RequestStatus.EN_EVALUACION);
			assertThat(entry.actorId()).isEqualTo(2);
			assertThat(entry.technicianId()).isNull();
			assertThat(entry.createdAt()).isEqualTo(NOW);
			assertThat(entry.reason()).isEqualTo("Se inició la evaluación del caso");
		});
	}

	@Test
	void assigningStoresTheTechnicianOfTheDirectory() {
		request(RequestStatus.EN_EVALUACION, null);

		ServiceRequest result = useCase.execute(SUPERVISOR, "SOL-2026-000001",
				new TransitionCommand(RequestStatus.ASIGNADA, null, 3L, null));

		assertThat(result.technicianId()).isEqualTo(3L);
		assertThat(history.getFirst().technicianId()).isEqualTo(3L);
		assertThat(history.getFirst().reason()).isEqualTo("Asignada a Carlos Mendoza");
	}

	@Test
	void aTechnicianOfAnotherAreaIsRejectedAndTheRequestDoesNotChange() {
		ServiceRequest before = request(RequestStatus.EN_EVALUACION, null);

		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, "SOL-2026-000001",
				new TransitionCommand(RequestStatus.ASIGNADA, null, 9L, null)))
				.isInstanceOf(InvalidTransitionDataException.class);

		assertThat(stored.get("SOL-2026-000001")).isEqualTo(before);
		assertThat(updates).isZero();
	}

	@Test
	void anUnknownTechnicianIsRejected() {
		request(RequestStatus.EN_EVALUACION, null);

		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, "SOL-2026-000001",
				new TransitionCommand(RequestStatus.ASIGNADA, null, 404L, null)))
				.isInstanceOf(InvalidTransitionDataException.class);
	}

	@Test
	void aForbiddenTransitionChangesNothing() {
		ServiceRequest before = request(RequestStatus.ASIGNADA, 7L); // technician 3 is not the assigned one

		assertThatThrownBy(() -> useCase.execute(TECHNICIAN, "SOL-2026-000001",
				new TransitionCommand(RequestStatus.EN_ATENCION, null, null, null)))
				.isInstanceOf(TransitionForbiddenException.class);

		assertThat(stored.get("SOL-2026-000001")).isEqualTo(before);
		assertThat(updates).isZero();
	}

	@Test
	void aConflictingTransitionChangesNothing() {
		ServiceRequest before = request(RequestStatus.REGISTRADA, null);

		assertThatThrownBy(() -> useCase.execute(SUPERVISOR, "SOL-2026-000001",
				new TransitionCommand(RequestStatus.RESUELTA, "Listo", null, null)))
				.isInstanceOf(InvalidTransitionException.class);

		assertThat(stored.get("SOL-2026-000001")).isEqualTo(before);
		assertThat(updates).isZero();
	}

	@Test
	void confirmingTheSolutionClosesWithTheConfirmedReason() {
		request(RequestStatus.RESUELTA, 3L);

		ServiceRequest result = useCase.execute(OWNER, "SOL-2026-000001",
				new TransitionCommand(RequestStatus.CERRADA, null, null, null));

		assertThat(result.status()).isEqualTo(RequestStatus.CERRADA);
		assertThat(result.closureReason()).isEqualTo(ClosureReason.CONFIRMADA);
		assertThat(result.technicianId()).isEqualTo(3L);
	}

}
