package pe.edu.unmsm.servicios.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.Area;
import pe.edu.unmsm.servicios.catalog.domain.Catalogs;
import pe.edu.unmsm.servicios.catalog.domain.Category;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;
import pe.edu.unmsm.servicios.dashboard.domain.DashboardAccessDeniedException;
import pe.edu.unmsm.servicios.dashboard.domain.Indicators;
import pe.edu.unmsm.servicios.dashboard.domain.InvalidDashboardQueryException;
import pe.edu.unmsm.servicios.dashboard.domain.Tally;
import pe.edu.unmsm.servicios.dashboard.domain.TechnicianLoad;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsQuery;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsRow;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.application.TechnicianDirectory;
import pe.edu.unmsm.servicios.workflow.domain.Technician;

class DashboardUseCasesTest {

	// 2026-10-26 10:00 in Lima.
	private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser(2, "s@unmsm.edu.pe", "Sup",
			"SUPERVISOR", 1L);
	private static final AuthenticatedUser SUPERVISOR_WITHOUT_AREA = new AuthenticatedUser(8, "x@unmsm.edu.pe", "X",
			"SUPERVISOR", null);
	private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "a@unmsm.edu.pe", "Admin",
			"ADMINISTRADOR");
	private static final AuthenticatedUser TECHNICIAN = new AuthenticatedUser(3, "t@unmsm.edu.pe", "Tec", "TECNICO",
			1L);
	private static final AuthenticatedUser REQUESTER = new AuthenticatedUser(4, "r@unmsm.edu.pe", "Req",
			"SOLICITANTE");

	private final List<Long> registeredAreas = new ArrayList<>();
	private final List<Instant[]> windows = new ArrayList<>();
	private final List<Long> openAreas = new ArrayList<>();
	private List<RequestAnalyticsRow> registered = List.of();
	private List<RequestAnalyticsRow> open = List.of();

	private final RequestAnalyticsQuery analytics = new RequestAnalyticsQuery() {

		@Override
		public List<RequestAnalyticsRow> registeredBetween(Long areaId, Instant from, Instant to) {
			registeredAreas.add(areaId);
			windows.add(new Instant[] { from, to });
			return registered;
		}

		@Override
		public List<RequestAnalyticsRow> openRequests(Long areaId) {
			openAreas.add(areaId);
			return open;
		}

	};

	private final CatalogLookup catalog = new CatalogLookup() {

		@Override
		public Catalogs activeCatalogs() {
			return new Catalogs(List.of(new Area(1, "Mantenimiento"), new Area(3, "Soporte Informático")), List.of(),
					List.of(new PriorityLevel(1, "Crítica", 8), new PriorityLevel(2, "Alta", 24),
							new PriorityLevel(3, "Media", 72), new PriorityLevel(4, "Baja", 120)),
					List.of());
		}

		@Override
		public Optional<Category> findActiveCategory(long id) {
			return Optional.empty();
		}

		@Override
		public Optional<PriorityLevel> findPriority(long id) {
			return Optional.empty();
		}

		@Override
		public Optional<Location> findLocation(long id) {
			return Optional.empty();
		}

	};

	private final TechnicianDirectory directory = new TechnicianDirectory() {

		@Override
		public Optional<Technician> findTechnician(long id) {
			return Optional.empty();
		}

		@Override
		public List<Technician> activeTechniciansOfArea(long areaId) {
			return areaId == 1 ? List.of(new Technician(3, "Ana", 1L, true), new Technician(9, "Beto", 1L, true))
					: List.of();
		}

	};

	private final GetDashboardIndicators indicators = new GetDashboardIndicators(analytics, catalog, CLOCK);
	private final GetTeamLoad team = new GetTeamLoad(directory, analytics, CLOCK, 8);

	private static RequestAnalyticsRow row(String code, RequestStatus status, Long technicianId, String technician,
			Instant createdAt, Instant dueAt, Instant firstResolvedAt) {
		return new RequestAnalyticsRow(code, status, "Electricidad", "Alta", 1, "Mantenimiento", technicianId,
				technician, "Facultad → Pabellón → Aula 101", createdAt, dueAt, firstResolvedAt);
	}

	// ───── Scope ─────

	@Test
	void aSupervisorAlwaysSeesTheirOwnAreaAndTheAreaIdIsIgnored() {
		Indicators result = indicators.execute(SUPERVISOR, "7", 3L);

		assertThat(registeredAreas).containsExactly(1L);
		assertThat(openAreas).containsExactly(1L);
		assertThat(result.areaName()).isEqualTo("Mantenimiento");
	}

	@Test
	void anAdministratorSeesEveryAreaByDefault() {
		Indicators result = indicators.execute(ADMIN, null, null);

		assertThat(registeredAreas).containsExactly((Long) null);
		assertThat(result.areaName()).isNull();
		assertThat(result.rangeDays()).isEqualTo(30);
	}

	@Test
	void anAdministratorCanFilterByArea() {
		Indicators result = indicators.execute(ADMIN, "mes", 3L);

		assertThat(registeredAreas).containsExactly(3L);
		assertThat(result.areaName()).isEqualTo("Soporte Informático");
		assertThat(result.rangeDays()).isEqualTo(26);
	}

	@Test
	void anUnknownAreaIsRejected() {
		assertThatThrownBy(() -> indicators.execute(ADMIN, "30", 99L))
				.isInstanceOf(InvalidDashboardQueryException.class)
				.satisfies(e -> assertThat(((InvalidDashboardQueryException) e).errors()).containsKey("areaId"));
	}

	@Test
	void anInvalidRangeIsRejected() {
		assertThatThrownBy(() -> indicators.execute(SUPERVISOR, "15", null))
				.isInstanceOf(InvalidDashboardQueryException.class);
	}

	@Test
	void otherRolesAndSupervisorsWithoutAreaCannotSeeIndicators() {
		for (AuthenticatedUser user : List.of(TECHNICIAN, REQUESTER, SUPERVISOR_WITHOUT_AREA)) {
			assertThatThrownBy(() -> indicators.execute(user, "30", null))
					.isInstanceOf(DashboardAccessDeniedException.class);
		}
		assertThat(registeredAreas).isEmpty();
	}

	// ───── Data flow ─────

	@Test
	void readsThePeriodAndThePreviousOneInASingleQuery() {
		indicators.execute(SUPERVISOR, "7", null);

		assertThat(windows).hasSize(1);
		assertThat(windows.getFirst()[0]).isEqualTo(Instant.parse("2026-10-13T05:00:00Z"));
		assertThat(windows.getFirst()[1]).isEqualTo(Instant.parse("2026-10-27T05:00:00Z"));
	}

	@Test
	void mapsTheRowsWithTheRulesOfTheRequestsModule() {
		Instant today = Instant.parse("2026-10-26T13:00:00Z");
		registered = List.of(
				// Resolved in 5 h, on time.
				row("A", RequestStatus.CERRADA, 3L, "Ana", today.minus(Duration.ofHours(10)),
						today.plus(Duration.ofHours(14)), today.minus(Duration.ofHours(5))),
				// Reopened after a resolution: pending, not attended.
				row("B", RequestStatus.EN_ATENCION, 3L, "Ana", today, today.plus(Duration.ofHours(24)), today),
				// Past its due date and still open: overdue.
				row("C", RequestStatus.REGISTRADA, null, null, today.minus(Duration.ofHours(9)),
						NOW.minus(Duration.ofMinutes(1)), null));
		open = List.of(registered.get(1), registered.get(2));

		Indicators result = indicators.execute(SUPERVISOR, "7", null);

		assertThat(result.kpis().registered().value()).isEqualTo(3);
		assertThat(result.kpis().attended().value()).isEqualTo(1);
		assertThat(result.kpis().pending().value()).isEqualTo(2);
		assertThat(result.kpis().overdue().value()).isEqualTo(1);
		assertThat(result.kpis().avgAttentionHours().value()).isEqualTo(5.0);
		assertThat(result.slaCompliancePct()).isEqualTo(100.0);
		assertThat(result.byPriority()).containsExactly(new Tally("Crítica", 0), new Tally("Alta", 3),
				new Tally("Media", 0), new Tally("Baja", 0));
		assertThat(result.byTechnician()).containsExactly(new Tally("Ana", 2), new Tally("Sin asignar", 1));
		assertThat(result.dueSoon()).extracting(r -> r.code()).containsExactly("B");
	}

	// ───── Team ─────

	@Test
	void theTeamIsTheActiveTechniciansOfTheSupervisorsAreaWithTheirLoad() {
		open = List.of(row("A", RequestStatus.ASIGNADA, 3L, "Ana", NOW, NOW.plus(Duration.ofHours(1)), null),
				row("B", RequestStatus.EN_ATENCION, 3L, "Ana", NOW, NOW.minus(Duration.ofHours(1)), null),
				row("C", RequestStatus.ASIGNADA, 9L, "Beto", NOW, NOW.plus(Duration.ofHours(1)), null));

		List<TechnicianLoad> loads = team.execute(SUPERVISOR);

		assertThat(openAreas).containsExactly(1L);
		assertThat(loads).containsExactly(new TechnicianLoad(3, "Ana", 1, 1, 1, 8),
				new TechnicianLoad(9, "Beto", 1, 0, 0, 8));
	}

	@Test
	void onlySupervisorsWithAnAreaCanSeeTheTeam() {
		for (AuthenticatedUser user : List.of(ADMIN, TECHNICIAN, REQUESTER, SUPERVISOR_WITHOUT_AREA)) {
			assertThatThrownBy(() -> team.execute(user)).isInstanceOf(DashboardAccessDeniedException.class);
		}
	}

	@Test
	void theCapacityMustBePositive() {
		assertThatThrownBy(() -> new GetTeamLoad(directory, analytics, CLOCK, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
