package pe.edu.unmsm.servicios.requests.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.Area;
import pe.edu.unmsm.servicios.catalog.domain.Catalogs;
import pe.edu.unmsm.servicios.catalog.domain.Category;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.LocationKind;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;
import pe.edu.unmsm.servicios.requests.domain.InvalidRequestException;
import pe.edu.unmsm.servicios.requests.domain.RequestCode;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

class CreateRequestTest {

	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");
	private static final AuthenticatedUser STUDENT = new AuthenticatedUser(4, "estudiante@unmsm.edu.pe",
			"Estudiante Demo", "SOLICITANTE");
	private static final String VALID = "El tomacorriente del aula no funciona desde ayer";

	private final List<ServiceRequest> saved = new ArrayList<>();
	private final List<RequestHistoryEntry> history = new ArrayList<>();
	private final List<Integer> yearsAsked = new ArrayList<>();

	private CreateRequest useCase(Instant now) {
		CatalogLookup catalog = new CatalogLookup() {

			@Override
			public Catalogs activeCatalogs() {
				return new Catalogs(List.of(new Area(1, "Mantenimiento")), List.of(), List.of(), List.of());
			}

			@Override
			public Optional<Category> findActiveCategory(long id) {
				return id == 1 ? Optional.of(new Category(1, "Electricidad", 1)) : Optional.empty();
			}

			@Override
			public Optional<PriorityLevel> findPriority(long id) {
				return switch ((int) id) {
					case 1 -> Optional.of(new PriorityLevel(1, "Crítica", 8));
					case 2 -> Optional.of(new PriorityLevel(2, "Alta", 24));
					default -> Optional.empty();
				};
			}

			@Override
			public Optional<Location> findLocation(long id) {
				return switch ((int) id) {
					case 3 -> Optional.of(new Location(3, "Aula 101", LocationKind.AMBIENTE, 2L));
					case 1 -> Optional.of(new Location(1, "Facultad", LocationKind.FACULTAD, null));
					default -> Optional.empty();
				};
			}

		};
		RequestCodeGenerator codes = year -> {
			yearsAsked.add(year);
			return new RequestCode(year, 7);
		};
		RequestRepository requests = (request, first) -> {
			ServiceRequest stored = new ServiceRequest(100L, request.code(), request.requesterId(),
					request.categoryId(), request.locationId(), request.description(), request.priorityId(),
					request.status(), request.areaId(), request.technicianId(), request.closureReason(),
					request.createdAt(), request.dueAt(), request.updatedAt());
			saved.add(stored);
			history.add(first);
			return stored;
		};
		return new CreateRequest(catalog, codes, requests, Clock.fixed(now, ZoneOffset.UTC));
	}

	private CreateRequest useCase() {
		return useCase(NOW);
	}

	@BeforeEach
	void clean() {
		saved.clear();
		history.clear();
		yearsAsked.clear();
	}

	@Test
	void registersTheRequestWithCodeStatusAreaAndDueDate() {
		ServiceRequest created = useCase().execute(STUDENT, new CreateRequest.Command(1, 3, VALID, 2));

		assertThat(created.code()).isEqualTo("SOL-2026-000007");
		assertThat(created.status()).isEqualTo(RequestStatus.REGISTRADA);
		assertThat(created.requesterId()).isEqualTo(4);
		assertThat(created.areaId()).as("area is the default area of the category").isEqualTo(1);
		assertThat(created.dueAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
		assertThat(saved).hasSize(1);
	}

	@Test
	void recordsTheFirstHistoryEntryMadeByTheRequester() {
		useCase().execute(STUDENT, new CreateRequest.Command(1, 3, VALID, 1));

		assertThat(history).singleElement().satisfies(entry -> {
			assertThat(entry.fromStatus()).isNull();
			assertThat(entry.toStatus()).isEqualTo(RequestStatus.REGISTRADA);
			assertThat(entry.actorId()).isEqualTo(4);
			assertThat(entry.createdAt()).isEqualTo(NOW);
		});
	}

	@Test
	void usesTheCriticalSlaOfEightHours() {
		ServiceRequest created = useCase().execute(STUDENT, new CreateRequest.Command(1, 3, VALID, 1));

		assertThat(created.dueAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
	}

	@Test
	void codeYearFollowsLimaTimeNotUtc() {
		// 2027-01-01T04:30Z is still 2026-12-31 23:30 in Lima (UTC-5).
		useCase(Instant.parse("2027-01-01T04:30:00Z")).execute(STUDENT, new CreateRequest.Command(1, 3, VALID, 2));

		assertThat(yearsAsked).containsExactly(2026);
	}

	@Test
	void rejectsDescriptionsThatAreTooShortOrTooLong() {
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(1, 3, "x".repeat(19), 2)))
				.isInstanceOf(InvalidRequestException.class)
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors()).containsKey("description"));
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(1, 3, "x".repeat(2001), 2)))
				.isInstanceOf(InvalidRequestException.class);
		assertThat(saved).isEmpty();
	}

	@Test
	void acceptsTheLimitsOf20And2000Characters() {
		useCase().execute(STUDENT, new CreateRequest.Command(1, 3, "x".repeat(20), 2));
		useCase().execute(STUDENT, new CreateRequest.Command(1, 3, "x".repeat(2000), 2));

		assertThat(saved).hasSize(2);
	}

	@Test
	void rejectsUnknownCategoryPriorityAndNonAmbienteLocation() {
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(99, 3, VALID, 2)))
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors()).containsOnlyKeys("categoryId"));
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(1, 3, VALID, 99)))
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors()).containsOnlyKeys("priorityId"));
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(1, 1, VALID, 2)))
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors()).containsOnlyKeys("locationId"));
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(1, 404, VALID, 2)))
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors()).containsOnlyKeys("locationId"));
		assertThat(saved).isEmpty();
	}

	@Test
	void reportsEveryInvalidFieldAtOnce() {
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(99, 1, "corta", 99)))
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors())
						.containsOnlyKeys("description", "categoryId", "locationId", "priorityId"));
	}

	@Test
	void doesNotConsumeACodeWhenTheDataIsInvalid() {
		assertThatThrownBy(() -> useCase().execute(STUDENT, new CreateRequest.Command(99, 3, VALID, 2)))
				.isInstanceOf(InvalidRequestException.class);

		assertThat(yearsAsked).isEmpty();
	}

}
