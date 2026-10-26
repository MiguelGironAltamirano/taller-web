package pe.edu.unmsm.servicios.requests.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

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

/** The extensions of the request detail (docs/api/ui-extensions.md) and the priority change. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class RequestDetailExtensionsApiTests {

	// Seeded by seed-dev.
	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	private static final String ADMIN = "admin@unmsm.edu.pe";
	// Inserted by these tests.
	private static final String SUPPORT_SUPERVISOR = "supervisor.soporte.ext@unmsm.edu.pe"; // area 3
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	private static final String VALID = "El tomacorriente del aula no funciona desde ayer";
	// Priorities of the seed: 1 Crítica (8 h), 2 Alta (24 h), 3 Media (72 h).
	private static final long CRITICAL = 1;
	private static final long MEDIUM = 3;

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		jdbc.update("delete from request_code_sequences");
		jdbc.update("insert ignore into users (full_name, email, password_hash, role, area_id, active, created_at) "
				+ "values ('Supervisor Soporte Ext', ?, ?, 'SUPERVISOR', 3, true, utc_timestamp(6))", SUPPORT_SUPERVISOR,
				DEMO_HASH);
	}

	private String createRequest() throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}",
				api.login(STUDENT));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return ApiClient.read(response, "$.code", String.class);
	}

	private void force(String code, String status, Long technicianId) {
		jdbc.update("update service_requests set status = ?, technician_id = ? where code = ?", status, technicianId,
				code);
	}

	private HttpResponse<String> detail(String user, String code) throws Exception {
		return api.get("/api/v1/requests/" + code, api.login(user));
	}

	private HttpResponse<String> changePriority(String user, String code, String json) throws Exception {
		return api.send("PATCH", "/api/v1/requests/" + code + "/priority", json, api.login(user));
	}

	private static List<String> actions(HttpResponse<String> response) {
		return ApiClient.read(response, "$.allowedActions");
	}

	// ───── allowedActions ─────

	@Test
	void eachUserIsOfferedTheActionsTheyMayPerform() throws Exception {
		String code = createRequest();

		assertThat(actions(detail(SUPERVISOR, code))).containsExactly("eval", "noproc", "dup", "prio");
		assertThat(actions(detail(STUDENT, code))).isEmpty();
		assertThat(actions(detail(ADMIN, code))).isEmpty();

		force(code, "ASIGNADA", 3L);
		assertThat(actions(detail(SUPERVISOR, code))).containsExactly("reassign", "prio");
		assertThat(actions(detail(TECHNICIAN, code))).containsExactly("start");

		force(code, "EN_ATENCION", 3L);
		assertThat(actions(detail(SUPERVISOR, code))).containsExactly("prio");
		assertThat(actions(detail(TECHNICIAN, code))).containsExactly("resolve");

		force(code, "RESUELTA", 3L);
		assertThat(actions(detail(SUPERVISOR, code))).isEmpty();
		assertThat(ApiClient.<List<String>>read(detail(STUDENT, code), "$.allowedTransitions"))
				.containsExactlyInAnyOrder("CERRADA", "EN_ATENCION");
	}

	@Test
	void aNewRequestHasEmptyExtensions() throws Exception {
		HttpResponse<String> response = detail(STUDENT, createRequest());

		assertThat(ApiClient.<Object>read(response, "$.solution")).isNull();
		assertThat(ApiClient.<Object>read(response, "$.history[0].note")).isNull();
	}

	// ───── PATCH /requests/{code}/priority ─────

	@Test
	void theSupervisorChangesThePriorityAndTheDueDateIsRecalculated() throws Exception {
		String code = createRequest();
		LocalDateTime created = jdbc.queryForObject("select created_at from service_requests where code = ?",
				LocalDateTime.class, code);

		HttpResponse<String> response = changePriority(SUPERVISOR, code, "{\"priorityId\":" + CRITICAL + "}");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(response, "$.priorityName")).isEqualTo("Crítica");
		assertThat(ApiClient.<String>read(response, "$.status")).isEqualTo("REGISTRADA");
		assertThat(jdbc.queryForObject("select due_at from service_requests where code = ?", LocalDateTime.class,
				code)).isEqualTo(created.plusHours(8));
		assertThat(ApiClient.<String>read(response, "$.history[1].note")).isEqualTo("Prioridad Alta → Prioridad Crítica");
		assertThat(ApiClient.<String>read(response, "$.history[1].fromStatus")).isEqualTo("REGISTRADA");
		assertThat(ApiClient.<String>read(response, "$.history[1].toStatus")).isEqualTo("REGISTRADA");
		assertThat(ApiClient.<String>read(response, "$.history[1].actorName")).isEqualTo("Supervisora Mantenimiento");
		assertThat(ApiClient.<String>read(response, "$.history[1].reason")).startsWith("Nuevo vencimiento: ");
	}

	@Test
	void aResolvedOrClosedRequestKeepsItsPriority() throws Exception {
		String code = createRequest();
		for (String status : List.of("RESUELTA", "CERRADA")) {
			force(code, status, 3L);
			ApiClient.assertProblem(changePriority(SUPERVISOR, code, "{\"priorityId\":" + MEDIUM + "}"), 409);
		}
		assertThat(jdbc.queryForObject("select priority_id from service_requests where code = ?", Long.class, code))
				.isEqualTo(2L);
	}

	@Test
	void onlyTheSupervisorOfTheAreaMayChangeIt() throws Exception {
		String code = createRequest();
		force(code, "ASIGNADA", 3L);

		ApiClient.assertProblem(changePriority(TECHNICIAN, code, "{\"priorityId\":1}"), 403);
		ApiClient.assertProblem(changePriority(STUDENT, code, "{\"priorityId\":1}"), 403);
		ApiClient.assertProblem(changePriority(SUPPORT_SUPERVISOR, code, "{\"priorityId\":1}"), 404);
		ApiClient.assertProblem(changePriority(SUPERVISOR, "SOL-2026-999999", "{\"priorityId\":1}"), 404);
	}

	@Test
	void anInvalidPriorityIsRejectedWithTheFieldError() throws Exception {
		String code = createRequest();

		for (String body : List.of("{}", "{\"priorityId\":999}", "{\"priorityId\":2}")) {
			HttpResponse<String> response = changePriority(SUPERVISOR, code, body);
			ApiClient.assertProblem(response, 400);
			assertThat(ApiClient.<String>read(response, "$.errors.priorityId")).as(body).isNotBlank();
		}
	}

}
