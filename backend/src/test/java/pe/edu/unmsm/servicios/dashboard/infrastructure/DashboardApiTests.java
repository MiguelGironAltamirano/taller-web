package pe.edu.unmsm.servicios.dashboard.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class DashboardApiTests {

	// Seeded by seed-dev.
	private static final String ADMIN = "admin@unmsm.edu.pe";
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	private static final String STUDENT = "estudiante@unmsm.edu.pe"; // id 4
	private static final long TECHNICIAN_ID = 3;

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;
	private Instant now;
	private int sequence;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		sequence = 0;
	}

	// ───── Access ─────

	@Test
	void onlySupervisorsAndAdministratorsCanSeeTheIndicators() throws Exception {
		assertThat(api.get("/api/v1/dashboard/indicators", api.login(SUPERVISOR)).statusCode()).isEqualTo(200);
		assertThat(api.get("/api/v1/dashboard/indicators", api.login(ADMIN)).statusCode()).isEqualTo(200);
		ApiClient.assertProblem(api.get("/api/v1/dashboard/indicators", api.login(TECHNICIAN)), 403);
		ApiClient.assertProblem(api.get("/api/v1/dashboard/indicators", api.login(STUDENT)), 403);
		assertThat(api.get("/api/v1/dashboard/indicators", null).statusCode()).isEqualTo(401);
	}

	@Test
	void onlySupervisorsCanSeeTheTeam() throws Exception {
		assertThat(api.get("/api/v1/dashboard/team", api.login(SUPERVISOR)).statusCode()).isEqualTo(200);
		ApiClient.assertProblem(api.get("/api/v1/dashboard/team", api.login(ADMIN)), 403);
		ApiClient.assertProblem(api.get("/api/v1/dashboard/team", api.login(TECHNICIAN)), 403);
		ApiClient.assertProblem(api.get("/api/v1/dashboard/team", api.login(STUDENT)), 403);
	}

	@Test
	void anInvalidRangeIsABadRequest() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/dashboard/indicators?range=15", api.login(SUPERVISOR));

		ApiClient.assertProblem(response, 400);
		assertThat(ApiClient.<Map<String, String>>read(response, "$.errors")).containsKey("range");
	}

	@Test
	void anUnknownAreaIsABadRequestForTheAdministrator() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/dashboard/indicators?areaId=99", api.login(ADMIN));

		ApiClient.assertProblem(response, 400);
		assertThat(ApiClient.<Map<String, String>>read(response, "$.errors")).containsKey("areaId");
	}

	@Test
	void anEmptyDatabaseGivesZerosAndOneValuePerDay() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/dashboard/indicators?range=7", api.login(ADMIN));

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.read(response, "$.rangeDays", Integer.class)).isEqualTo(7);
		assertThat(ApiClient.<Object>read(response, "$.areaName")).isNull();
		assertThat(ApiClient.read(response, "$.kpis.registered.value", Double.class)).isZero();
		assertThat(ApiClient.<List<Integer>>read(response, "$.timeline")).hasSize(7).containsOnly(0);
		assertThat(ApiClient.<List<Object>>read(response, "$.kpis.overdue.series")).hasSize(7);
		assertThat(ApiClient.<List<Integer>>read(response, "$.byPriority[*].value")).containsExactly(0, 0, 0, 0);
		assertThat(ApiClient.<List<Object>>read(response, "$.dueSoon")).isEmpty();
	}

	// ───── A realistic scenario ─────

	private void scenario() {
		// Area 1 (Mantenimiento).
		insert(1, 2, "ASIGNADA", TECHNICIAN_ID, ago(1), now.plus(Duration.ofHours(23)), null); // due soon
		insert(1, 2, "CERRADA", TECHNICIAN_ID, ago(48), ago(24), ago(44)); // attended in 4 h, on time
		insert(1, 2, "EN_ATENCION", TECHNICIAN_ID, ago(72), ago(1), null); // overdue
		insert(2, 3, "CERRADA", null, ago(50), ago(-22), null); // closed as NO_PROCEDE: never attended
		insert(1, 2, "RESUELTA", TECHNICIAN_ID, ago(240), ago(216), ago(238)); // previous 7-day period
		// Area 3 (Soporte Informático).
		insert(5, 2, "REGISTRADA", null, ago(48), ago(24), null);
	}

	@Test
	void theSupervisorSeesTheIndicatorsOfTheirAreaEvenIfAskingForAnother() throws Exception {
		scenario();

		HttpResponse<String> response = api.get("/api/v1/dashboard/indicators?range=7&areaId=3",
				api.login(SUPERVISOR));

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.read(response, "$.areaName", String.class)).isEqualTo("Mantenimiento");
		assertThat(ApiClient.read(response, "$.kpis.registered.value", Double.class)).isEqualTo(4);
		assertThat(ApiClient.read(response, "$.kpis.registered.deltaPct", Double.class)).isEqualTo(300);
		assertThat(ApiClient.read(response, "$.kpis.pending.value", Double.class)).isEqualTo(2);
		assertThat(ApiClient.read(response, "$.kpis.attended.value", Double.class)).isEqualTo(1);
		assertThat(ApiClient.read(response, "$.kpis.resolvedPct.value", Double.class)).isEqualTo(25);
		assertThat(ApiClient.read(response, "$.kpis.avgAttentionHours.value", Double.class)).isEqualTo(4);
		assertThat(ApiClient.read(response, "$.kpis.overdue.value", Double.class)).isEqualTo(1);
		assertThat(ApiClient.read(response, "$.slaCompliancePct", Double.class)).isEqualTo(100);
		assertThat(ApiClient.<List<Integer>>read(response, "$.timeline")).hasSize(7)
				.satisfies(days -> assertThat(days.stream().mapToInt(Integer::intValue).sum()).isEqualTo(4));
		assertThat(ApiClient.<List<String>>read(response, "$.byPriority[*].label")).containsExactly("Crítica",
				"Alta", "Media", "Baja");
		assertThat(ApiClient.<List<Integer>>read(response, "$.byPriority[*].value")).containsExactly(0, 3, 1, 0);
		assertThat(ApiClient.<List<String>>read(response, "$.byCategory[*].label")).containsExactly("Electricidad",
				"Gasfitería");
		assertThat(ApiClient.<List<String>>read(response, "$.byTechnician[*].label"))
				.containsExactly("Técnico Mantenimiento", "Sin asignar");
		assertThat(ApiClient.<List<String>>read(response, "$.dueSoon[*].code")).containsExactly("SOL-2026-900001");
		assertThat(ApiClient.read(response, "$.dueSoon[0].categoryName", String.class)).isEqualTo("Electricidad");
		assertThat(ApiClient.read(response, "$.dueSoon[0].priorityName", String.class)).isEqualTo("Alta");
		assertThat(ApiClient.read(response, "$.dueSoon[0].status", String.class)).isEqualTo("ASIGNADA");
		assertThat(ApiClient.read(response, "$.dueSoon[0].locationName", String.class)).contains("Aula 101");
		assertThat(ApiClient.read(response, "$.dueSoon[0].overdue", Boolean.class)).isFalse();
		assertThat(ApiClient.read(response, "$.dueSoon[0].dueAt", String.class)).endsWith("Z");
	}

	@Test
	void theAdministratorSeesEveryAreaOrTheOneAskedFor() throws Exception {
		scenario();
		String token = api.login(ADMIN);

		HttpResponse<String> all = api.get("/api/v1/dashboard/indicators?range=30", token);
		HttpResponse<String> support = api.get("/api/v1/dashboard/indicators?range=30&areaId=3", token);
		HttpResponse<String> month = api.get("/api/v1/dashboard/indicators?range=mes", token);

		assertThat(ApiClient.<Object>read(all, "$.areaName")).isNull();
		assertThat(ApiClient.read(all, "$.rangeDays", Integer.class)).isEqualTo(30);
		assertThat(ApiClient.read(all, "$.kpis.registered.value", Double.class)).isEqualTo(6);
		assertThat(ApiClient.read(support, "$.areaName", String.class)).isEqualTo("Soporte Informático");
		assertThat(ApiClient.read(support, "$.kpis.registered.value", Double.class)).isEqualTo(1);
		assertThat(ApiClient.read(support, "$.byTechnician[0].label", String.class)).isEqualTo("Sin asignar");
		int dayOfMonth = LocalDateTime.ofInstant(now, java.time.ZoneId.of("America/Lima")).getDayOfMonth();
		assertThat(ApiClient.read(month, "$.rangeDays", Integer.class)).isEqualTo(dayOfMonth);
		assertThat(ApiClient.<List<Object>>read(month, "$.timeline")).hasSize(dayOfMonth);
	}

	@Test
	void theTeamShowsTheCurrentLoadOfEachTechnicianOfTheArea() throws Exception {
		scenario();

		HttpResponse<String> response = api.get("/api/v1/dashboard/team", api.login(SUPERVISOR));

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		String tech = "$[?(@.id == " + TECHNICIAN_ID + ")]";
		assertThat(ApiClient.<List<String>>read(response, tech + ".fullName")).containsExactly("Técnico Mantenimiento");
		assertThat(ApiClient.<List<Integer>>read(response, tech + ".assigned")).containsExactly(1);
		assertThat(ApiClient.<List<Integer>>read(response, tech + ".inProgress")).containsExactly(1);
		assertThat(ApiClient.<List<Integer>>read(response, tech + ".overdue")).containsExactly(1);
		assertThat(ApiClient.<List<Integer>>read(response, "$[*].capacity")).containsOnly(8);
	}

	private Instant ago(long hours) {
		return now.minus(Duration.ofHours(hours));
	}

	private void insert(long categoryId, long priorityId, String status, Long technicianId, Instant createdAt,
			Instant dueAt, Instant resolvedAt) {
		long areaId = jdbc.queryForObject("select default_area_id from categories where id = ?", Long.class,
				categoryId);
		String code = "SOL-2026-9%05d".formatted(++sequence);
		jdbc.update("""
				insert into service_requests (code, requester_id, category_id, location_id, description, priority_id,
				  status, area_id, technician_id, created_at, due_at, updated_at)
				values (?, 4, ?, 3, 'Descripción de prueba', ?, ?, ?, ?, ?, ?, ?)
				""", code, categoryId, priorityId, status, areaId, technicianId, utc(createdAt), utc(dueAt),
				utc(createdAt));
		long id = jdbc.queryForObject("select id from service_requests where code = ?", Long.class, code);
		history(id, null, "REGISTRADA", createdAt);
		if (resolvedAt != null) {
			history(id, "EN_ATENCION", "RESUELTA", resolvedAt);
		}
	}

	private void history(long requestId, String from, String to, Instant at) {
		jdbc.update("insert into request_history (request_id, from_status, to_status, actor_id, created_at) "
				+ "values (?, ?, ?, 2, ?)", requestId, from, to, utc(at));
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
