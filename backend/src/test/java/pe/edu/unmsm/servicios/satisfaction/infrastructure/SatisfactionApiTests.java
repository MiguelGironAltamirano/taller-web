package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class SatisfactionApiTests {

	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe";
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe";
	private static final String OTHER_STUDENT = "otra.solicitante2@unmsm.edu.pe";
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";
	private static final String VALID_DESC = "El tomacorriente del aula no funciona desde ayer";

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from ratings");
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		jdbc.update("delete from request_code_sequences");
		jdbc.update("insert ignore into users (full_name, email, password_hash, role, requester_type, area_id, active, created_at) "
				+ "values ('Otra Solicitante 2', ?, ?, 'SOLICITANTE', 'DOCENTE', null, true, utc_timestamp(6))",
				OTHER_STUDENT, DEMO_HASH);
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	private String createAndResolve(String student) throws Exception {
		String code = createRequest(student);
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");
		transition(SUPERVISOR, code, "{\"to\":\"ASIGNADA\",\"technicianId\":3}");
		transition(TECHNICIAN, code, "{\"to\":\"EN_ATENCION\"}");
		transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"Reparado\"}");
		return code;
	}

	private String createRequest(String student) throws Exception {
		HttpResponse<String> r = api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID_DESC + "\",\"priorityId\":2}",
				api.login(student));
		assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
		return ApiClient.read(r, "$.code", String.class);
	}

	private HttpResponse<String> transition(String user, String code, String json) throws Exception {
		return api.post("/api/v1/requests/" + code + "/transitions", json, api.login(user));
	}

	private HttpResponse<String> rate(String student, String code, String json) throws Exception {
		return api.post("/api/v1/requests/" + code + "/rating", json, api.login(student));
	}

	// ── 201 ──────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("Rating 1–5 without token returns 401")
	void withoutTokenReturns401() throws Exception {
		HttpResponse<String> r = api.post("/api/v1/requests/SOL-2026-000001/rating",
				"{\"score\":4}", null);
		assertThat(r.statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("Valid score 3 saves the rating and returns 201")
	void validScoreReturns201() throws Exception {
		String code = createAndResolve(STUDENT);

		HttpResponse<String> r = rate(STUDENT, code, "{\"score\":3,\"comment\":\"Buen servicio\"}");

		assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
		assertThat(ApiClient.<Integer>read(r, "$.score")).isEqualTo(3);
		assertThat(ApiClient.<String>read(r, "$.comment")).isEqualTo("Buen servicio");
		assertThat(ApiClient.<Object>read(r, "$.id")).isNotNull();
	}

	@Test
	@DisplayName("Rating without comment is accepted")
	void ratingWithoutCommentIsAccepted() throws Exception {
		String code = createAndResolve(STUDENT);

		HttpResponse<String> r = rate(STUDENT, code, "{\"score\":5}");

		assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
	}

	// ── 400 ──────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("Score 0 returns 400")
	void score0Returns400() throws Exception {
		String code = createAndResolve(STUDENT);
		assertThat(rate(STUDENT, code, "{\"score\":0}").statusCode()).isEqualTo(400);
	}

	@Test
	@DisplayName("Score 6 returns 400")
	void score6Returns400() throws Exception {
		String code = createAndResolve(STUDENT);
		assertThat(rate(STUDENT, code, "{\"score\":6}").statusCode()).isEqualTo(400);
	}

	@Test
	@DisplayName("Missing score returns 400")
	void missingScoreReturns400() throws Exception {
		String code = createAndResolve(STUDENT);
		assertThat(rate(STUDENT, code, "{\"comment\":\"ok\"}").statusCode()).isEqualTo(400);
	}

	// ── 404 ──────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("Rating someone else's request returns 404")
	void ratingAnotherStudentsRequestReturns404() throws Exception {
		String code = createAndResolve(STUDENT);

		HttpResponse<String> r = rate(OTHER_STUDENT, code, "{\"score\":4}");

		assertThat(r.statusCode()).isEqualTo(404);
	}

	@Test
	@DisplayName("Rating a non-existent request returns 404")
	void ratingNonExistentRequestReturns404() throws Exception {
		assertThat(rate(STUDENT, "SOL-9999-999999", "{\"score\":4}").statusCode()).isEqualTo(404);
	}

	// ── 409 ──────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("Rating a request not in RESUELTA returns 409")
	void ratingNotResolvedRequestReturns409() throws Exception {
		String code = createRequest(STUDENT);

		HttpResponse<String> r = rate(STUDENT, code, "{\"score\":4}");

		assertThat(r.statusCode()).isEqualTo(409);
	}

	@Test
	@DisplayName("Rating the same request twice returns 409")
	void ratingTwiceReturns409() throws Exception {
		String code = createAndResolve(STUDENT);
		assertThat(rate(STUDENT, code, "{\"score\":4}").statusCode()).isEqualTo(201);

		HttpResponse<String> r = rate(STUDENT, code, "{\"score\":5}");
		assertThat(r.statusCode()).isEqualTo(409);
	}

}
