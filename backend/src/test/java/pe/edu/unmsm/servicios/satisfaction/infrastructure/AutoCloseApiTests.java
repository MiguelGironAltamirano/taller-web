package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
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
import pe.edu.unmsm.servicios.satisfaction.application.AutoCloseResolvedRequests;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/** The 72 h auto-close against MySQL; the age of a resolution is simulated by moving its history entry back. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class AutoCloseApiTests {

	private static final String STUDENT = "estudiante@unmsm.edu.pe"; // id 4
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe";
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3
	private static final String VALID = "El tomacorriente del aula no funciona desde ayer";

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	AutoCloseResolvedRequests autoClose;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		jdbc.update("delete from request_code_sequences");
	}

	private String resolvedHoursAgo(int hours) throws Exception {
		String code = ApiClient.read(api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}",
				api.login(STUDENT)), "$.code", String.class);
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");
		transition(SUPERVISOR, code, "{\"to\":\"ASIGNADA\",\"technicianId\":3}");
		transition(TECHNICIAN, code, "{\"to\":\"EN_ATENCION\"}");
		transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el tomacorriente\"}");
		backdateResolution(code, hours);
		return code;
	}

	private void backdateResolution(String code, int hours) {
		jdbc.update("update request_history h join service_requests r on r.id = h.request_id "
				+ "set h.created_at = utc_timestamp(6) - interval ? hour where r.code = ? and h.to_status = 'RESUELTA'",
				hours, code);
	}

	private void transition(String user, String code, String json) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/transitions", json, api.login(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
	}

	private String status(String code) {
		return jdbc.queryForObject("select status from service_requests where code = ?", String.class, code);
	}

	private int historyCount(String code) {
		return jdbc.queryForObject("select count(*) from request_history h join service_requests r "
				+ "on r.id = h.request_id where r.code = ?", Integer.class, code);
	}

	@Test
	void aRequestResolvedMoreThan72HoursAgoIsClosedAutomatically() throws Exception {
		String code = resolvedHoursAgo(73);

		assertThat(autoClose.run()).isEqualTo(1);

		HttpResponse<String> detail = api.get("/api/v1/requests/" + code, api.login(SUPERVISOR));
		assertThat(ApiClient.<String>read(detail, "$.status")).isEqualTo("CERRADA");
		assertThat(ApiClient.<String>read(detail, "$.closureReason")).isEqualTo("AUTOMATICA");
		assertThat(ApiClient.<Object>read(detail, "$.rating")).isNull();
		assertThat(ApiClient.<List<String>>read(detail, "$.history[*].toStatus")).last().isEqualTo("CERRADA");
		assertThat(ApiClient.<List<String>>read(detail, "$.history[*].actorName")).last().isEqualTo("Sistema");
		assertThat(ApiClient.<List<String>>read(detail, "$.history[*].reason")).last()
				.isEqualTo("Cierre automático: sin respuesta del solicitante en 72 horas");
		assertThat(ApiClient.<List<Object>>read(detail, "$.allowedTransitions")).isEmpty();
	}

	@Test
	void theRequesterIsNotifiedOfTheAutomaticClosure() throws Exception {
		String code = resolvedHoursAgo(80);

		autoClose.run();

		List<String> texts = jdbc.queryForList("select n.text from notifications n join users u on u.id = n.recipient_id "
				+ "where u.email = ?", String.class, STUDENT);
		assertThat(texts).contains("La solicitud " + code + " se cerró automáticamente tras 72 horas sin respuesta.");
	}

	@Test
	void aRequestResolvedLessThan72HoursAgoDoesNotChange() throws Exception {
		String code = resolvedHoursAgo(71);
		int history = historyCount(code);

		assertThat(autoClose.run()).isZero();

		assertThat(status(code)).isEqualTo("RESUELTA");
		assertThat(historyCount(code)).isEqualTo(history);
	}

	@Test
	void anAlreadyClosedRequestIsNotProcessedAgain() throws Exception {
		String code = resolvedHoursAgo(100);
		transition(STUDENT, code, "{\"to\":\"CERRADA\"}");
		int history = historyCount(code);

		assertThat(autoClose.run()).isZero();

		assertThat(jdbc.queryForObject("select closure_reason from service_requests where code = ?", String.class,
				code)).isEqualTo("CONFIRMADA");
		assertThat(historyCount(code)).isEqualTo(history);
	}

	@Test
	void runningTwiceClosesOnlyOnce() throws Exception {
		String code = resolvedHoursAgo(73);

		assertThat(autoClose.run()).isEqualTo(1);
		int history = historyCount(code);
		assertThat(autoClose.run()).isZero();

		assertThat(historyCount(code)).isEqualTo(history);
	}

	@Test
	void theClockRestartsWhenARejectedSolutionIsResolvedAgain() throws Exception {
		String code = resolvedHoursAgo(100);
		transition(STUDENT, code, "{\"to\":\"EN_ATENCION\",\"reason\":\"El tomacorriente sigue sin funcionar\"}");
		transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el cableado\"}");

		assertThat(autoClose.run()).isZero();

		assertThat(status(code)).isEqualTo("RESUELTA");
	}

	@Test
	void anAutomaticallyClosedRequestCannotBeRated() throws Exception {
		String code = resolvedHoursAgo(73);
		autoClose.run();

		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/rating", "{\"score\":4}",
				api.login(STUDENT));

		assertThat(response.statusCode()).isEqualTo(409);
	}

}
