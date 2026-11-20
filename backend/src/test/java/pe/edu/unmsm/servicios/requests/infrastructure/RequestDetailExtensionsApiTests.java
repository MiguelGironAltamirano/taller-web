package pe.edu.unmsm.servicios.requests.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
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
		// The owner may cancel it until a technician is assigned (contract «Cancelar y restaurar una solicitud»).
		assertThat(actions(detail(STUDENT, code))).containsExactly("cancel");
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
		assertThat(ApiClient.<List<Object>>read(response, "$.comments")).isEmpty();
		assertThat(ApiClient.<List<Object>>read(response, "$.evidences")).isEmpty();
	}

	// ───── autoCloseAt ─────

	private void transition(String user, String code, String json) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/transitions", json, api.login(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
	}

	@Test
	void aResolvedRequestTellsWhenItWillBeClosedAutomatically() throws Exception {
		String code = createRequest();
		assertThat(ApiClient.<Object>read(detail(STUDENT, code), "$.autoCloseAt")).isNull();
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");
		transition(SUPERVISOR, code, "{\"to\":\"ASIGNADA\",\"technicianId\":3}");
		transition(TECHNICIAN, code, "{\"to\":\"EN_ATENCION\"}");
		transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el tomacorriente\"}");
		Integer previousHours = jdbc.queryForObject("select auto_close_hours from system_settings where id = 1",
				Integer.class);
		try {
			jdbc.update("update system_settings set auto_close_hours = 48 where id = 1");

			HttpResponse<String> response = detail(STUDENT, code);

			List<String> created = ApiClient.read(response, "$.history[*].createdAt");
			assertThat(Instant.parse(ApiClient.read(response, "$.autoCloseAt", String.class)))
					.isEqualTo(Instant.parse(created.getLast()).plus(Duration.ofHours(48)));
		}
		finally {
			jdbc.update("update system_settings set auto_close_hours = ? where id = 1", previousHours);
		}
	}

	// ───── comments and evidences (collaboration module) ─────

	@Test
	void theDetailIncludesTheCommentsAndEvidencesOfTheRequest() throws Exception {
		String code = createRequest();
		HttpResponse<String> comment = api.post("/api/v1/requests/" + code + "/comments",
				"{\"text\":\"Adjunto la foto del tomacorriente\"}", api.login(STUDENT));
		assertThat(comment.statusCode()).as(comment.body()).isEqualTo(201);
		jdbc.update("insert into evidences (request_id, uploader_id, file_name, stored_name, kind, size_bytes, "
				+ "created_at) select id, requester_id, 'foto.jpg', '00000000-0000-0000-0000-000000000001', 'IMG', "
				+ "2048, utc_timestamp(6) from service_requests where code = ?", code);

		HttpResponse<String> response = detail(SUPERVISOR, code);

		assertThat(ApiClient.<List<String>>read(response, "$.comments[*].text"))
				.containsExactly("Adjunto la foto del tomacorriente");
		assertThat(ApiClient.<String>read(response, "$.comments[0].authorName")).isEqualTo("Estudiante Demo");
		assertThat(ApiClient.<String>read(response, "$.comments[0].createdAt")).isNotBlank();
		assertThat(ApiClient.<String>read(response, "$.evidences[0].fileName")).isEqualTo("foto.jpg");
		assertThat(ApiClient.<String>read(response, "$.evidences[0].kind")).isEqualTo("IMG");
		assertThat(ApiClient.<Integer>read(response, "$.evidences[0].sizeBytes")).isEqualTo(2048);
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

	// ───── what the requester sees (docs/api/contrato-v1.md, "Prioridad que ve el solicitante") ─────

	@Test
	void theRequesterKeepsSeeingThePriorityTheyChoseAndItsEstimatedDate() throws Exception {
		String code = createRequest();
		force(code, "ASIGNADA", 3L);
		assertThat(changePriority(SUPERVISOR, code, "{\"priorityId\":" + MEDIUM + "}").statusCode()).isEqualTo(200);

		HttpResponse<String> requester = detail(STUDENT, code);
		Instant created = Instant.parse(ApiClient.read(requester, "$.createdAt", String.class));
		assertThat(ApiClient.<String>read(requester, "$.priorityName")).isEqualTo("Alta");
		assertThat(Instant.parse(ApiClient.read(requester, "$.dueAt", String.class)))
				.as("estimated date: registration + 24 h of Alta").isEqualTo(created.plus(Duration.ofHours(24)));
		assertThat(ApiClient.<Boolean>read(requester, "$.estimatedDue")).isTrue();
		assertThat(ApiClient.<Boolean>read(requester, "$.overdue")).isFalse();
		assertThat(ApiClient.<List<String>>read(requester, "$.history[*].note")).allMatch(note -> note == null);

		for (String staff : List.of(SUPERVISOR, TECHNICIAN, ADMIN)) {
			HttpResponse<String> response = detail(staff, code);
			assertThat(ApiClient.<String>read(response, "$.priorityName")).as(staff).isEqualTo("Media");
			assertThat(Instant.parse(ApiClient.read(response, "$.dueAt", String.class))).as(staff)
					.isEqualTo(created.plus(Duration.ofHours(72)));
			assertThat(ApiClient.<Boolean>read(response, "$.estimatedDue")).as(staff).isFalse();
			assertThat(ApiClient.<List<String>>read(response, "$.history[*].note")).as(staff)
					.contains("Prioridad Alta → Prioridad Media");
		}
	}

	@Test
	void theRequesterInboxListsFiltersAndCountsWithTheChosenPriority() throws Exception {
		String code = createRequest();
		assertThat(changePriority(SUPERVISOR, code, "{\"priorityId\":" + CRITICAL + "}").statusCode()).isEqualTo(200);
		// Even past the estimated date the requester does not see it as overdue.
		jdbc.update("update service_requests set due_at = utc_timestamp(6) - interval 1 hour, "
				+ "requested_due_at = utc_timestamp(6) - interval 1 hour where code = ?", code);
		String token = api.login(STUDENT);

		HttpResponse<String> list = api.get("/api/v1/requests", token);
		assertThat(ApiClient.<String>read(list, "$.items[0].priorityName")).isEqualTo("Alta");
		assertThat(ApiClient.<String>read(list, "$.items[0].dueAt")).isNotBlank();
		assertThat(ApiClient.<Boolean>read(list, "$.items[0].estimatedDue")).isTrue();
		assertThat(ApiClient.<Boolean>read(list, "$.items[0].overdue")).isFalse();
		assertThat(ApiClient.<Integer>read(api.get("/api/v1/requests?priorityId=2", token), "$.total")).isEqualTo(1);
		assertThat(ApiClient.<Integer>read(api.get("/api/v1/requests?priorityId=1", token), "$.total")).isZero();
		assertThat(ApiClient.<Integer>read(api.get("/api/v1/requests?overdue=false", token), "$.total")).isEqualTo(1);
		assertThat(ApiClient.<Integer>read(api.get("/api/v1/requests/counts", token), "$.overdue")).isZero();

		String supervisor = api.login(SUPERVISOR);
		assertThat(ApiClient.<String>read(api.get("/api/v1/requests", supervisor), "$.items[0].priorityName"))
				.isEqualTo("Crítica");
		assertThat(ApiClient.<Integer>read(api.get("/api/v1/requests?priorityId=1", supervisor), "$.total"))
				.isEqualTo(1);
		assertThat(ApiClient.<Integer>read(api.get("/api/v1/requests/counts", supervisor), "$.overdue")).isEqualTo(1);
	}

	@Test
	void theChosenPriorityIsStoredApartAndNeverChanges() throws Exception {
		String code = createRequest();
		changePriority(SUPERVISOR, code, "{\"priorityId\":" + CRITICAL + "}");
		changePriority(SUPERVISOR, code, "{\"priorityId\":" + MEDIUM + "}");

		assertThat(jdbc.queryForObject("select requested_priority_id from service_requests where code = ?", Long.class,
				code)).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select priority_id from service_requests where code = ?", Long.class, code))
				.isEqualTo(MEDIUM);
		LocalDateTime created = jdbc.queryForObject("select created_at from service_requests where code = ?",
				LocalDateTime.class, code);
		assertThat(jdbc.queryForObject("select requested_due_at from service_requests where code = ?",
				LocalDateTime.class, code)).as("estimated date of Alta, fixed").isEqualTo(created.plusHours(24));
		assertThat(jdbc.queryForList("select h.internal from request_history h join service_requests r "
				+ "on r.id = h.request_id where r.code = ? and h.note is not null", Boolean.class, code))
				.containsExactly(true, true);
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
