package pe.edu.unmsm.servicios.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

/** Cancelling and restoring a request, docs/api/contrato-v1.md «Cancelar y restaurar una solicitud». */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class CancelRestoreApiTests {

	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // area 1
	private static final long TECHNICIAN_ID = 3; // tecnico.mantenimiento, area 1
	private static final String PASSWORD = "Clave12345";
	private static final String CANCEL = "{\"to\":\"CANCELADA\",\"reason\":\"Ya lo arreglaron por su cuenta\"}";

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;
	private String requester;
	private String supervisor;

	@BeforeEach
	void setUp() throws Exception {
		api = new ApiClient(port);
		requester = newRequester();
		supervisor = api.login(SUPERVISOR);
	}

	/** A fresh requester per test: its requests and notifications belong to that test only. */
	private String newRequester() throws Exception {
		String email = "c" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@unmsm.edu.pe";
		api.post("/api/v1/auth/register", "{\"fullName\":\"Carla Cancela\",\"email\":\"" + email + "\",\"password\":\""
				+ PASSWORD + "\",\"requesterType\":\"ESTUDIANTE\"}", null);
		HttpResponse<String> login = api.post("/api/v1/auth/login",
				"{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}", null);
		return ApiClient.read(login, "$.accessToken", String.class);
	}

	private String createRequest() throws Exception {
		HttpResponse<String> created = api.post("/api/v1/requests", "{\"categoryId\":1,\"locationId\":3,"
				+ "\"description\":\"El tomacorriente del aula no funciona desde ayer\",\"priorityId\":2}", requester);
		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		return ApiClient.read(created, "$.code", String.class);
	}

	private HttpResponse<String> transition(String token, String code, String json) throws Exception {
		return api.post("/api/v1/requests/" + code + "/transitions", json, token);
	}

	private HttpResponse<String> detail(String token, String code) throws Exception {
		return api.get("/api/v1/requests/" + code, token);
	}

	private void evaluate(String code) throws Exception {
		assertThat(transition(supervisor, code, "{\"to\":\"EN_EVALUACION\"}").statusCode()).isEqualTo(200);
	}

	/** Moves the moment of the cancellation back, as if it had happened {@code ago}. */
	private void cancelledAgo(String code, Duration ago) {
		// In SQL and in UTC, like the application stores it (a JDBC timestamp would use the JVM time zone).
		jdbc.update("update request_history h join service_requests r on r.id = h.request_id "
				+ "set h.created_at = utc_timestamp(6) - interval ? second "
				+ "where r.code = ? and h.to_status = 'CANCELADA'", ago.toSeconds(), code);
	}

	/** The due date as {@code token} sees it: the SLA for the staff, the estimated date for the requester. */
	private Instant dueAt(String token, String code) throws Exception {
		return Instant.parse(ApiClient.read(detail(token, code), "$.dueAt", String.class));
	}

	@Test
	void theRequesterIsOfferedToCancelUntilATechnicianIsAssigned() throws Exception {
		String code = createRequest();
		assertThat(ApiClient.<List<String>>read(detail(requester, code), "$.allowedActions")).contains("cancel");

		evaluate(code);
		assertThat(ApiClient.<List<String>>read(detail(requester, code), "$.allowedActions")).contains("cancel");

		assertThat(transition(supervisor, code, "{\"to\":\"ASIGNADA\",\"technicianId\":" + TECHNICIAN_ID + "}")
				.statusCode()).isEqualTo(200);
		assertThat(ApiClient.<List<String>>read(detail(requester, code), "$.allowedActions")).doesNotContain("cancel");
		ApiClient.assertProblem(transition(requester, code, CANCEL), 409);
	}

	@Test
	void cancellingRecordsTheReasonAndOffersToRestore() throws Exception {
		String code = createRequest();

		HttpResponse<String> cancelled = transition(requester, code, CANCEL);

		assertThat(cancelled.statusCode()).as(cancelled.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(cancelled, "$.status")).isEqualTo("CANCELADA");
		assertThat(ApiClient.<String>read(cancelled, "$.history[-1].reason")).contains("Ya lo arreglaron por su cuenta");
		HttpResponse<String> seen = detail(requester, code);
		assertThat(ApiClient.<List<String>>read(seen, "$.allowedActions")).containsExactly("restore");
		assertThat(ApiClient.<List<String>>read(seen, "$.allowedTransitions")).containsExactly("REGISTRADA");
	}

	@Test
	void cancellingNeedsAReason() throws Exception {
		String code = createRequest();

		HttpResponse<String> withoutReason = transition(requester, code, "{\"to\":\"CANCELADA\"}");

		ApiClient.assertProblem(withoutReason, 400);
		assertThat(ApiClient.<String>read(withoutReason, "$.errors.reason")).isNotBlank();
	}

	@Test
	void onlyTheRequesterWhoOwnsItCanCancel() throws Exception {
		String code = createRequest();

		ApiClient.assertProblem(transition(newRequester(), code, CANCEL), 403);
		ApiClient.assertProblem(transition(supervisor, code, CANCEL), 403);
	}

	@Test
	void theSupervisorLearnsThatARequestInEvaluationWasCancelled() throws Exception {
		String code = createRequest();
		evaluate(code);

		transition(requester, code, CANCEL);

		assertThat(api.get("/api/v1/notifications", supervisor).body()).contains(code).contains("Cancelada");
	}

	@Test
	void restoringBringsItBackWhereItWasWithTheTimeItWasCancelledAdded() throws Exception {
		String code = createRequest();
		evaluate(code);
		Instant dueBefore = dueAt(supervisor, code);
		Instant estimatedBefore = dueAt(requester, code);
		transition(requester, code, CANCEL);
		cancelledAgo(code, Duration.ofDays(2));

		HttpResponse<String> restored = transition(requester, code, "{\"to\":\"EN_EVALUACION\"}");

		assertThat(restored.statusCode()).as(restored.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(restored, "$.status")).isEqualTo("EN_EVALUACION");
		// Both dates move by the time it was cancelled: the SLA the staff sees and the estimated date of the requester.
		assertThat(Duration.between(dueBefore, dueAt(supervisor, code))).isBetween(Duration.ofDays(2).minusMinutes(1),
				Duration.ofDays(2).plusMinutes(1));
		assertThat(Duration.between(estimatedBefore, dueAt(requester, code)))
				.isBetween(Duration.ofDays(2).minusMinutes(1), Duration.ofDays(2).plusMinutes(1));
		assertThat(ApiClient.<List<String>>read(detail(requester, code), "$.allowedActions")).contains("cancel");
	}

	@Test
	void itCanOnlyGoBackToTheStatusItHad() throws Exception {
		String code = createRequest();
		transition(requester, code, CANCEL);

		ApiClient.assertProblem(transition(requester, code, "{\"to\":\"EN_EVALUACION\"}"), 409);
		ApiClient.assertProblem(transition(supervisor, code, "{\"to\":\"REGISTRADA\"}"), 403);
	}

	@Test
	void afterSevenDaysItCanNoLongerBeRestored() throws Exception {
		String code = createRequest();
		transition(requester, code, CANCEL);
		cancelledAgo(code, Duration.ofDays(7).plusMinutes(1));

		HttpResponse<String> late = transition(requester, code, "{\"to\":\"REGISTRADA\"}");

		ApiClient.assertProblem(late, 409);
		assertThat(ApiClient.<String>read(late, "$.detail"))
				.isEqualTo("Pasaron más de 7 días desde que la cancelaste; ya no se puede restaurar");
		assertThat(ApiClient.<List<String>>read(detail(requester, code), "$.allowedActions")).isEmpty();
	}

	@Test
	void aCancelledRequestIsNeitherOverdueNorPendingNorReprioritized() throws Exception {
		String code = createRequest();
		transition(requester, code, CANCEL);
		jdbc.update("update service_requests set due_at = utc_timestamp(6) - interval 1 day where code = ?", code);

		// Overdue is a staff notion (the requester never sees it): checked as the supervisor of the area.
		assertThat(ApiClient.<Boolean>read(detail(supervisor, code), "$.overdue")).isFalse();
		assertThat(api.get("/api/v1/requests?overdue=true", supervisor).body()).doesNotContain(code);
		assertThat(api.get("/api/v1/requests?status=CANCELADA", requester).body()).contains(code);
		assertThat(ApiClient.<Integer>read(api.get("/api/v1/requests/counts", requester), "$.byStatus.CANCELADA"))
				.isEqualTo(1);
		ApiClient.assertProblem(api.send("PATCH", "/api/v1/requests/" + code + "/priority", "{\"priorityId\":1}",
				supervisor), 409);
	}

}
