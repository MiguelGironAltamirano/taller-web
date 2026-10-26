package pe.edu.unmsm.servicios.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;
import pe.edu.unmsm.servicios.workflow.application.AutoCloseUnconfirmedRequests;
import pe.edu.unmsm.servicios.workflow.infrastructure.scheduling.AutoCloseJobAccess;

/**
 * The automatic closure against the real database. The scheduled job is off in tests, so each test runs the use case
 * directly after moving the resolution back in time.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class AutoCloseApiTests {

	// Seeded by seed-dev.
	private static final String STUDENT = "estudiante@unmsm.edu.pe"; // id 4
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // id 2, area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	private static final String VALID = "El tomacorriente del aula no funciona desde ayer";

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	AutoCloseUnconfirmedRequests autoClose;

	@Autowired
	ApplicationContext context;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		jdbc.update("delete from request_code_sequences");
		jdbc.update("update system_settings set auto_close_hours = 72 where id = 1");
	}

	private String resolvedRequest() throws Exception {
		HttpResponse<String> created = api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}",
				api.login(STUDENT));
		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		String code = ApiClient.read(created, "$.code", String.class);
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");
		transition(SUPERVISOR, code, "{\"to\":\"ASIGNADA\",\"technicianId\":3}");
		transition(TECHNICIAN, code, "{\"to\":\"EN_ATENCION\"}");
		transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el tomacorriente\"}");
		return code;
	}

	private void transition(String user, String code, String json) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/transitions", json, api.login(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
	}

	/** Moves every past entry into RESUELTA of the request {@code hours} back in time. */
	private void resolvedHoursAgo(String code, int hours) {
		jdbc.update("update request_history h join service_requests r on r.id = h.request_id "
				+ "set h.created_at = utc_timestamp(6) - interval ? hour "
				+ "where r.code = ? and h.to_status = 'RESUELTA'", hours, code);
	}

	private Map<String, Object> request(String code) {
		return jdbc.queryForMap("select id, status, closure_reason from service_requests where code = ?", code);
	}

	private List<String> notificationsOf(long userId, String code) {
		return jdbc.queryForList("select n.text from notifications n join service_requests r on r.id = n.request_id "
				+ "where n.recipient_id = ? and r.code = ? order by n.id", String.class, userId, code);
	}

	@Test
	void aSolutionUnconfirmedForSeventyTwoHoursIsClosedBySystemAndEveryoneIsNotified() throws Exception {
		String code = resolvedRequest();
		resolvedHoursAgo(code, 73);

		assertThat(autoClose.run()).isEqualTo(1);

		assertThat(request(code)).containsEntry("status", "CERRADA").containsEntry("closure_reason", "AUTOMATICA");
		Map<String, Object> last = jdbc.queryForMap("select h.from_status, h.to_status, h.actor_id, h.reason "
				+ "from request_history h join service_requests r on r.id = h.request_id where r.code = ? "
				+ "order by h.id desc limit 1", code);
		assertThat(last).containsEntry("from_status", "RESUELTA").containsEntry("to_status", "CERRADA")
				.containsEntry("actor_id", null)
				.containsEntry("reason", "Cierre automático: el solicitante no confirmó la solución en 72 h");

		HttpResponse<String> detail = api.get("/api/v1/requests/" + code, api.login(STUDENT));
		assertThat(detail.statusCode()).as(detail.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(detail, "$.status")).isEqualTo("CERRADA");
		assertThat(ApiClient.<String>read(detail, "$.closureReason")).isEqualTo("AUTOMATICA");
		assertThat(ApiClient.<String>read(detail, "$.solution")).isEqualTo("Se cambió el tomacorriente");
		List<String> actors = ApiClient.read(detail, "$.history[*].actorName");
		assertThat(actors.getLast()).isEqualTo("Sistema");
		List<String> transitions = ApiClient.read(detail, "$.allowedTransitions");
		assertThat(transitions).isEmpty();

		String text = "La solicitud " + code
				+ " se cerró automáticamente porque la solución no se confirmó a tiempo.";
		assertThat(notificationsOf(4, code)).contains(text);
		assertThat(notificationsOf(3, code)).contains(text);
		assertThat(notificationsOf(2, code)).contains(text);

		HttpResponse<String> rating = api.post("/api/v1/requests/" + code + "/rating", "{\"score\":5}",
				api.login(STUDENT));
		ApiClient.assertProblem(rating, 409);
	}

	@Test
	void aSolutionWithinThePeriodIsKeptAndRunningAgainClosesNothingTwice() throws Exception {
		String recent = resolvedRequest();
		resolvedHoursAgo(recent, 71);
		String expired = resolvedRequest();
		resolvedHoursAgo(expired, 100);

		assertThat(autoClose.run()).isEqualTo(1);
		assertThat(autoClose.run()).isZero();

		assertThat(request(recent)).containsEntry("status", "RESUELTA");
		assertThat(request(expired)).containsEntry("status", "CERRADA");
		Integer closures = jdbc.queryForObject("select count(*) from request_history h join service_requests r "
				+ "on r.id = h.request_id where r.code = ? and h.to_status = 'CERRADA'", Integer.class, expired);
		assertThat(closures).isEqualTo(1);
	}

	@Test
	void thePeriodCountsFromTheLatestResolutionNotFromTheCreation() throws Exception {
		String code = resolvedRequest();
		resolvedHoursAgo(code, 100);
		transition(STUDENT, code, "{\"to\":\"EN_ATENCION\",\"reason\":\"Sigue sin funcionar\"}");
		jdbc.update("update service_requests set created_at = utc_timestamp(6) - interval 200 hour where code = ?",
				code);
		transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el cableado\"}");

		assertThat(autoClose.run()).isZero();
		assertThat(request(code)).containsEntry("status", "RESUELTA");
	}

	@Test
	void thePeriodIsTheAdministratorSetting() throws Exception {
		String code = resolvedRequest();
		resolvedHoursAgo(code, 5);
		assertThat(autoClose.run()).isZero();

		jdbc.update("update system_settings set auto_close_hours = 4 where id = 1");

		assertThat(autoClose.run()).isEqualTo(1);
		assertThat(request(code)).containsEntry("status", "CERRADA");
	}

	@Test
	void theAuditListsTheSystemAsTheActor() throws Exception {
		String code = resolvedRequest();
		resolvedHoursAgo(code, 73);
		autoClose.run();

		HttpResponse<String> audit = api.get("/api/v1/admin/audit?q=Sistema", api.login("admin@unmsm.edu.pe"));

		assertThat(audit.statusCode()).as(audit.body()).isEqualTo(200);
		assertThat(audit.body()).contains(code).contains("Sistema");
	}

	@Test
	void theScheduledJobIsOffInTests() {
		assertThat(AutoCloseJobAccess.isRegistered(context)).isFalse();
	}

}
