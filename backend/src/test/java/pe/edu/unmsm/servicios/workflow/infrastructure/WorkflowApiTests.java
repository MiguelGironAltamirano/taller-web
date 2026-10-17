package pe.edu.unmsm.servicios.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
class WorkflowApiTests {

	// Seeded by seed-dev.
	private static final String STUDENT = "estudiante@unmsm.edu.pe"; // id 4
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // id 2, area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	private static final String ADMIN = "admin@unmsm.edu.pe";
	// Inserted by these tests.
	private static final String OTHER_STUDENT = "otra.solicitante@unmsm.edu.pe";
	private static final String SUPPORT_SUPERVISOR = "supervisor.soporte@unmsm.edu.pe"; // area 3
	private static final String SECOND_TECHNICIAN = "segundo.tecnico@unmsm.edu.pe"; // area 1
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	// Ids of the users inserted by setUp: users.id is AUTO_INCREMENT and other test classes register users through the
	// API on the same database, so fixed ids may already be taken.
	private long supportTechnicianId; // area 3
	private long secondTechnicianId; // area 1
	private long inactiveTechnicianId; // area 1, inactive
	private static final String VALID = "El tomacorriente del aula no funciona desde ayer";

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
		insertUser("Otra Solicitante", OTHER_STUDENT, "SOLICITANTE", "DOCENTE", null, true);
		insertUser("Supervisor Soporte", SUPPORT_SUPERVISOR, "SUPERVISOR", null, 3, true);
		supportTechnicianId = insertUser("Técnico Soporte", "tecnico.soporte@unmsm.edu.pe", "TECNICO", null, 3, true);
		secondTechnicianId = insertUser("Segundo Técnico", SECOND_TECHNICIAN, "TECNICO", null, 1, true);
		inactiveTechnicianId = insertUser("Técnico Inactivo", "tecnico.inactivo@unmsm.edu.pe", "TECNICO", null, 1, false);
	}

	/** Inserts the user if missing, restores its state if a previous test changed it, and returns its id. */
	private long insertUser(String name, String email, String role, String requesterType, Integer areaId,
			boolean active) {
		jdbc.update("insert ignore into users (full_name, email, password_hash, role, requester_type, area_id, "
				+ "active, created_at) values (?, ?, ?, ?, ?, ?, ?, utc_timestamp(6))", name, email, DEMO_HASH, role,
				requesterType, areaId, active);
		jdbc.update("update users set full_name = ?, role = ?, requester_type = ?, area_id = ?, active = ? "
				+ "where email = ?", name, role, requesterType, areaId, active, email);
		return jdbc.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private String createRequest(String user) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}", api.login(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return ApiClient.read(response, "$.code", String.class);
	}

	private HttpResponse<String> transition(String user, String code, String json) throws Exception {
		return api.post("/api/v1/requests/" + code + "/transitions", json, api.login(user));
	}

	private HttpResponse<String> detail(String user, String code) throws Exception {
		return api.get("/api/v1/requests/" + code, api.login(user));
	}

	private String status(String code) {
		return jdbc.queryForObject("select status from service_requests where code = ?", String.class, code);
	}

	private int historyCount(String code) {
		return jdbc.queryForObject("select count(*) from request_history h join service_requests r "
				+ "on r.id = h.request_id where r.code = ?", Integer.class, code);
	}

	private void force(String code, String status, Long technicianId) {
		jdbc.update("update service_requests set status = ?, technician_id = ? where code = ?", status, technicianId,
				code);
	}

	/** The reason of the most recent history entry (JsonPath slices return lists). */
	private static String lastReason(HttpResponse<String> response) {
		List<String> reasons = ApiClient.read(response, "$.history[*].reason");
		return reasons.getLast();
	}

	private static String to(String status) {
		return "{\"to\":\"" + status + "\"}";
	}

	private static String assign(long technicianId) {
		return "{\"to\":\"ASIGNADA\",\"technicianId\":" + technicianId + "}";
	}

	// ───── The complete flow ─────

	@Test
	void theCompleteFlowLeavesSixHistoryEntriesWithUserDateAndStatuses() throws Exception {
		String code = createRequest(STUDENT);

		assertThat(transition(SUPERVISOR, code, to("EN_EVALUACION")).statusCode()).isEqualTo(200);
		HttpResponse<String> assigned = transition(SUPERVISOR, code, assign(3));
		assertThat(assigned.statusCode()).as(assigned.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(assigned, "$.technician.fullName")).isEqualTo("Técnico Mantenimiento");
		assertThat(transition(TECHNICIAN, code, to("EN_ATENCION")).statusCode()).isEqualTo(200);
		HttpResponse<String> resolved = transition(TECHNICIAN, code,
				"{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el tomacorriente\"}");
		assertThat(resolved.statusCode()).as(resolved.body()).isEqualTo(200);
		HttpResponse<String> closed = transition(STUDENT, code, to("CERRADA"));
		assertThat(closed.statusCode()).as(closed.body()).isEqualTo(200);

		HttpResponse<String> response = detail(SUPERVISOR, code);
		assertThat(ApiClient.<String>read(response, "$.status")).isEqualTo("CERRADA");
		assertThat(ApiClient.<String>read(response, "$.closureReason")).isEqualTo("CONFIRMADA");
		assertThat(ApiClient.<List<String>>read(response, "$.history[*].toStatus")).containsExactly("REGISTRADA",
				"EN_EVALUACION", "ASIGNADA", "EN_ATENCION", "RESUELTA", "CERRADA");
		assertThat(ApiClient.<List<String>>read(response, "$.history[*].actorName")).containsExactly("Estudiante Demo",
				"Supervisora Mantenimiento", "Supervisora Mantenimiento", "Técnico Mantenimiento",
				"Técnico Mantenimiento", "Estudiante Demo");
		assertThat(ApiClient.<Object>read(response, "$.history[0].fromStatus")).isNull();
		assertThat(ApiClient.<List<String>>read(response, "$.history[1:].fromStatus")).containsExactly("REGISTRADA",
				"EN_EVALUACION", "ASIGNADA", "EN_ATENCION", "RESUELTA");
		assertThat(ApiClient.<List<String>>read(response, "$.history[*].createdAt")).doesNotContainNull().hasSize(6);
		assertThat(ApiClient.<String>read(response, "$.history[4].reason")).isEqualTo("Solución: Se cambió el tomacorriente");
		assertThat(ApiClient.<List<Object>>read(response, "$.allowedTransitions")).isEmpty();
		assertThat(historyCount(code)).isEqualTo(6);
	}

	// ───── 409: outside the table ─────

	@Test
	void aTransitionOutsideTheTableIsAConflictAndTheRequestDoesNotChange() throws Exception {
		String code = createRequest(STUDENT);

		ApiClient.assertProblem(transition(SUPERVISOR, code, "{\"to\":\"RESUELTA\",\"reason\":\"saltando pasos\"}"), 409);
		ApiClient.assertProblem(transition(SUPERVISOR, code, to("REGISTRADA")), 409);
		ApiClient.assertProblem(transition(SUPERVISOR, code, assign(3)), 409);
		ApiClient.assertProblem(transition(STUDENT, code, to("RESUELTA")), 409);

		assertThat(status(code)).isEqualTo("REGISTRADA");
		assertThat(historyCount(code)).isEqualTo(1);
	}

	@Test
	void aClosedRequestAdmitsNoTransition() throws Exception {
		String code = createRequest(STUDENT);
		force(code, "CERRADA", null);

		for (String target : List.of("REGISTRADA", "EN_EVALUACION", "ASIGNADA", "EN_ATENCION", "RESUELTA")) {
			ApiClient.assertProblem(transition(SUPERVISOR, code, "{\"to\":\"" + target + "\",\"reason\":\"intento\",\"technicianId\":3}"),
					409);
		}
		assertThat(status(code)).isEqualTo("CERRADA");
	}

	// ───── 403: not the right person ─────

	@Test
	void anUnassignedTechnicianCannotStartAttending() throws Exception {
		String code = createRequest(STUDENT);
		force(code, "ASIGNADA", secondTechnicianId);

		ApiClient.assertProblem(transition(TECHNICIAN, code, to("EN_ATENCION")), 403);

		assertThat(status(code)).isEqualTo("ASIGNADA");
		assertThat(historyCount(code)).isEqualTo(1);
	}

	@Test
	void onlyTheRightRoleMayPerformEachTransition() throws Exception {
		String code = createRequest(STUDENT);

		ApiClient.assertProblem(transition(STUDENT, code, to("EN_EVALUACION")), 403);
		ApiClient.assertProblem(transition(TECHNICIAN, code, to("EN_EVALUACION")), 403);
		ApiClient.assertProblem(transition(ADMIN, code, to("EN_EVALUACION")), 403);
		ApiClient.assertProblem(transition(SUPPORT_SUPERVISOR, code, to("EN_EVALUACION")), 403);
		assertThat(status(code)).isEqualTo("REGISTRADA");

		force(code, "RESUELTA", 3L);
		ApiClient.assertProblem(transition(OTHER_STUDENT, code, to("CERRADA")), 403);
		ApiClient.assertProblem(transition(SUPERVISOR, code, to("CERRADA")), 403);
		assertThat(status(code)).isEqualTo("RESUELTA");
	}

	// ───── 400: required data ─────

	@Test
	void assigningWithoutATechnicianOrWithOneOfAnotherAreaOrInactiveIsABadRequest() throws Exception {
		String code = createRequest(STUDENT);
		force(code, "EN_EVALUACION", null);

		HttpResponse<String> none = transition(SUPERVISOR, code, to("ASIGNADA"));
		ApiClient.assertProblem(none, 400);
		assertThat(ApiClient.<String>read(none, "$.errors.technicianId")).isNotBlank();
		ApiClient.assertProblem(transition(SUPERVISOR, code, assign(supportTechnicianId)), 400);
		ApiClient.assertProblem(transition(SUPERVISOR, code, assign(inactiveTechnicianId)), 400);
		ApiClient.assertProblem(transition(SUPERVISOR, code, assign(999)), 400);
		ApiClient.assertProblem(transition(SUPERVISOR, code, assign(4)), 400); // a requester is not a technician

		assertThat(status(code)).isEqualTo("EN_EVALUACION");
		assertThat(historyCount(code)).isEqualTo(1);
	}

	@Test
	void reassigningRequiresAReasonAndChangesTheTechnician() throws Exception {
		String code = createRequest(STUDENT);
		force(code, "ASIGNADA", 3L);

		HttpResponse<String> noReason = transition(SUPERVISOR, code,
				"{\"to\":\"ASIGNADA\",\"technicianId\":" + secondTechnicianId + "}");
		ApiClient.assertProblem(noReason, 400);
		assertThat(ApiClient.<String>read(noReason, "$.errors.reason")).isNotBlank();

		HttpResponse<String> ok = transition(SUPERVISOR, code,
				"{\"to\":\"ASIGNADA\",\"technicianId\":" + secondTechnicianId + ",\"reason\":\"Carga de trabajo\"}");
		assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(ok, "$.status")).isEqualTo("ASIGNADA");
		assertThat(ApiClient.<String>read(ok, "$.technician.fullName")).isEqualTo("Segundo Técnico");
		assertThat(lastReason(ok)).isEqualTo(
				"Reasignada a Segundo Técnico. Motivo: Carga de trabajo");
		// The new technician now sees it; the previous one no longer does.
		assertThat(detail(SECOND_TECHNICIAN, code).statusCode()).isEqualTo(200);
		assertThat(detail(TECHNICIAN, code).statusCode()).isEqualTo(404);
	}

	@Test
	void resolvingWithoutASolutionCommentIsABadRequest() throws Exception {
		String code = createRequest(STUDENT);
		force(code, "EN_ATENCION", 3L);

		ApiClient.assertProblem(transition(TECHNICIAN, code, to("RESUELTA")), 400);
		ApiClient.assertProblem(transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"   \"}"), 400);
		assertThat(status(code)).isEqualTo("EN_ATENCION");
	}

	@Test
	void aMissingOrUnknownTargetStatusIsABadRequest() throws Exception {
		String code = createRequest(STUDENT);

		ApiClient.assertProblem(transition(SUPERVISOR, code, "{}"), 400);
		ApiClient.assertProblem(transition(SUPERVISOR, code, to("INEXISTENTE")), 400);
		assertThat(status(code)).isEqualTo("REGISTRADA");
	}

	// ───── Closing without resolving ─────

	@Test
	void theSupervisorClosesAsNoProcedeOrDuplicadaOnlyFromRegistradaOrEnEvaluacion() throws Exception {
		String first = createRequest(STUDENT);
		HttpResponse<String> noProcede = transition(SUPERVISOR, first,
				"{\"to\":\"CERRADA\",\"closureReason\":\"NO_PROCEDE\",\"reason\":\"Fuera del alcance\"}");
		assertThat(noProcede.statusCode()).as(noProcede.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(noProcede, "$.status")).isEqualTo("CERRADA");
		assertThat(ApiClient.<String>read(noProcede, "$.closureReason")).isEqualTo("NO_PROCEDE");
		assertThat(lastReason(noProcede)).isEqualTo("No procede: Fuera del alcance");

		String second = createRequest(STUDENT);
		force(second, "EN_EVALUACION", null);
		HttpResponse<String> duplicate = transition(SUPERVISOR, second,
				"{\"to\":\"CERRADA\",\"closureReason\":\"DUPLICADA\"}");
		assertThat(ApiClient.<String>read(duplicate, "$.closureReason")).isEqualTo("DUPLICADA");

		String assigned = createRequest(STUDENT);
		force(assigned, "ASIGNADA", 3L);
		ApiClient.assertProblem(transition(SUPERVISOR, assigned,
				"{\"to\":\"CERRADA\",\"closureReason\":\"NO_PROCEDE\"}"), 409);
		assertThat(status(assigned)).isEqualTo("ASIGNADA");
	}

	@Test
	void theContractStyleClosureReasonInTheReasonFieldIsAccepted() throws Exception {
		String code = createRequest(STUDENT);

		HttpResponse<String> response = transition(SUPERVISOR, code, "{\"to\":\"CERRADA\",\"reason\":\"DUPLICADA\"}");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(response, "$.closureReason")).isEqualTo("DUPLICADA");
	}

	@Test
	void closingWithoutResolvingNeedsAClosureReasonAndOnlyTheSupervisorOfTheAreaMayDoIt() throws Exception {
		String code = createRequest(STUDENT);

		ApiClient.assertProblem(transition(SUPERVISOR, code, to("CERRADA")), 400);
		ApiClient.assertProblem(transition(SUPERVISOR, code, "{\"to\":\"CERRADA\",\"closureReason\":\"CONFIRMADA\"}"), 400);
		String json = "{\"to\":\"CERRADA\",\"closureReason\":\"NO_PROCEDE\"}";
		ApiClient.assertProblem(transition(SUPPORT_SUPERVISOR, code, json), 403);
		ApiClient.assertProblem(transition(TECHNICIAN, code, json), 403);
		ApiClient.assertProblem(transition(STUDENT, code, json), 403);
		assertThat(status(code)).isEqualTo("REGISTRADA");
	}

	// ───── Rejecting the solution ─────

	@Test
	void rejectingTheSolutionReturnsToEnAtencionAndRequiresAReason() throws Exception {
		String code = createRequest(STUDENT);
		force(code, "RESUELTA", 3L);

		ApiClient.assertProblem(transition(STUDENT, code, to("EN_ATENCION")), 400);
		assertThat(status(code)).isEqualTo("RESUELTA");

		HttpResponse<String> response = transition(STUDENT, code,
				"{\"to\":\"EN_ATENCION\",\"reason\":\"Sigue la fuga en el techo\"}");
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(response, "$.status")).isEqualTo("EN_ATENCION");
		assertThat(ApiClient.<String>read(response, "$.technician.fullName")).isEqualTo("Técnico Mantenimiento");
		assertThat(lastReason(response))
				.isEqualTo("Solución rechazada: Sigue la fuga en el techo");
		assertThat(ApiClient.<Object>read(response, "$.closureReason")).isNull();
	}

	// ───── Allowed transitions in the detail ─────

	@Test
	void theDetailListsOnlyTheTransitionsEachUserMayPerform() throws Exception {
		String code = createRequest(STUDENT);

		assertThat(ApiClient.<List<String>>read(detail(SUPERVISOR, code), "$.allowedTransitions"))
				.containsExactlyInAnyOrder("EN_EVALUACION", "CERRADA");
		assertThat(ApiClient.<List<String>>read(detail(STUDENT, code), "$.allowedTransitions")).isEmpty();
		assertThat(ApiClient.<List<String>>read(detail(ADMIN, code), "$.allowedTransitions")).isEmpty();

		force(code, "ASIGNADA", 3L);
		assertThat(ApiClient.<List<String>>read(detail(TECHNICIAN, code), "$.allowedTransitions"))
				.containsExactly("EN_ATENCION");
		assertThat(ApiClient.<List<String>>read(detail(SUPERVISOR, code), "$.allowedTransitions"))
				.containsExactly("ASIGNADA");

		force(code, "RESUELTA", 3L);
		assertThat(ApiClient.<List<String>>read(detail(STUDENT, code), "$.allowedTransitions"))
				.containsExactlyInAnyOrder("CERRADA", "EN_ATENCION");
		assertThat(ApiClient.<List<String>>read(detail(TECHNICIAN, code), "$.allowedTransitions")).isEmpty();
	}

	@Test
	void everyAllowedTransitionIsAcceptedWhenAttemptedWithTheRightData() throws Exception {
		// What the detail offers must actually work: the client only shows buttons for these statuses.
		String code = createRequest(STUDENT);
		force(code, "EN_EVALUACION", null);
		List<String> offered = ApiClient.read(detail(SUPERVISOR, code), "$.allowedTransitions");
		assertThat(offered).containsExactlyInAnyOrder("ASIGNADA", "CERRADA");
		assertThat(transition(SUPERVISOR, code, assign(3)).statusCode()).isEqualTo(200);
	}

	// ───── Unknown request, no token ─────

	@Test
	void anUnknownRequestIsNotFoundAndAMissingTokenIsUnauthorized() throws Exception {
		ApiClient.assertProblem(transition(SUPERVISOR, "SOL-2026-999999", to("EN_EVALUACION")), 404);
		ApiClient.assertProblem(api.post("/api/v1/requests/SOL-2026-000001/transitions", to("EN_EVALUACION"), null), 401);
	}

	// ───── Concurrency ─────

	@Test
	void twoSimultaneousIdenticalTransitionsOnlyOneWins() throws Exception {
		String code = createRequest(STUDENT);
		String token = api.login(SUPERVISOR);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < 2; i++) {
				Callable<Integer> task = () -> {
					start.await();
					return api.post("/api/v1/requests/" + code + "/transitions", to("EN_EVALUACION"), token).statusCode();
				};
				futures.add(pool.submit(task));
			}
			start.countDown();
			List<Integer> codes = new ArrayList<>();
			for (Future<Integer> future : futures) {
				codes.add(future.get());
			}

			assertThat(codes).containsExactlyInAnyOrder(200, 409);
			assertThat(historyCount(code)).as("registration + one evaluation").isEqualTo(2);
			assertThat(status(code)).isEqualTo("EN_EVALUACION");
		}
		finally {
			pool.shutdownNow();
		}
	}

	// ───── GET /users/technicians ─────

	@Test
	void aSupervisorListsTheActiveTechniciansOfTheirAreaWithTheirLoad() throws Exception {
		String busy = createRequest(STUDENT);
		String busyToo = createRequest(STUDENT);
		String done = createRequest(STUDENT);
		force(busy, "ASIGNADA", 3L);
		force(busyToo, "EN_ATENCION", 3L);
		force(done, "RESUELTA", 3L); // resolved work no longer counts as load

		HttpResponse<String> response = api.get("/api/v1/users/technicians?areaId=1", api.login(SUPERVISOR));

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.<List<String>>read(response, "$[*].fullName"))
				.containsExactlyInAnyOrder("Técnico Mantenimiento", "Segundo Técnico");
		assertThat(ApiClient.<List<Integer>>read(response, "$[?(@.id==3)].activeRequests")).containsExactly(2);
		assertThat(ApiClient.<List<Integer>>read(response, "$[?(@.id==" + secondTechnicianId + ")].activeRequests"))
				.containsExactly(0);
	}

	@Test
	void theAreaDefaultsToTheOneOfTheSupervisor() throws Exception {
		HttpResponse<String> own = api.get("/api/v1/users/technicians", api.login(SUPERVISOR));
		HttpResponse<String> support = api.get("/api/v1/users/technicians", api.login(SUPPORT_SUPERVISOR));

		assertThat(ApiClient.<List<Object>>read(own, "$")).hasSize(2);
		assertThat(ApiClient.<List<String>>read(support, "$[*].fullName")).containsExactly("Técnico Soporte");
	}

	@Test
	void technicianListingIsRestrictedToSupervisorsAndTheirOwnArea() throws Exception {
		ApiClient.assertProblem(api.get("/api/v1/users/technicians?areaId=3", api.login(SUPERVISOR)), 403);
		ApiClient.assertProblem(api.get("/api/v1/users/technicians?areaId=1", api.login(STUDENT)), 403);
		ApiClient.assertProblem(api.get("/api/v1/users/technicians?areaId=1", api.login(TECHNICIAN)), 403);
		ApiClient.assertProblem(api.get("/api/v1/users/technicians?areaId=1", null), 401);
	}

}
