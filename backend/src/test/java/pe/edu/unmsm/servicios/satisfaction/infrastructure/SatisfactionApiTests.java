package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

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

/** POST /requests/{code}/rating as the contract defines it (docs/api/ui-extensions.md). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class SatisfactionApiTests {

	// Seeded by seed-dev.
	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	// Inserted by these tests.
	private static final String OTHER_STUDENT = "otro.calificador@unmsm.edu.pe";
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";
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
		jdbc.update("insert ignore into users (full_name, email, password_hash, role, requester_type, area_id, active, "
				+ "created_at) values ('Otro Calificador', ?, ?, 'SOLICITANTE', 'DOCENTE', null, true, utc_timestamp(6))",
				OTHER_STUDENT, DEMO_HASH);
	}

	private String resolved() throws Exception {
		String code = ApiClient.read(api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}",
				api.login(STUDENT)), "$.code", String.class);
		ok(transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}"));
		ok(transition(SUPERVISOR, code, "{\"to\":\"ASIGNADA\",\"technicianId\":3}"));
		ok(transition(TECHNICIAN, code, "{\"to\":\"EN_ATENCION\"}"));
		ok(transition(TECHNICIAN, code, "{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el tomacorriente\"}"));
		return code;
	}

	private String confirmed() throws Exception {
		String code = resolved();
		ok(transition(STUDENT, code, "{\"to\":\"CERRADA\"}"));
		return code;
	}

	private HttpResponse<String> transition(String user, String code, String json) throws Exception {
		return api.post("/api/v1/requests/" + code + "/transitions", json, api.login(user));
	}

	private HttpResponse<String> rate(String user, String code, String json) throws Exception {
		return api.post("/api/v1/requests/" + code + "/rating", json, user == null ? null : api.login(user));
	}

	private static void ok(HttpResponse<String> response) {
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
	}

	private int ratingsOf(String code) {
		return jdbc.queryForObject("select count(*) from ratings t join service_requests r on r.id = t.request_id "
				+ "where r.code = ?", Integer.class, code);
	}

	// ───── 200 ─────

	@Test
	void theOwnerRatesAConfirmedRequestAndGetsTheDetailWithTheRating() throws Exception {
		String code = confirmed();

		HttpResponse<String> response = rate(STUDENT, code, "{\"score\":4,\"comment\":\"  Rápido y amable  \"}");

		ok(response);
		assertThat(ApiClient.<String>read(response, "$.code")).isEqualTo(code);
		assertThat(ApiClient.<String>read(response, "$.status")).isEqualTo("CERRADA");
		assertThat(ApiClient.<String>read(response, "$.closureReason")).isEqualTo("CONFIRMADA");
		assertThat(ApiClient.<Integer>read(response, "$.rating.score")).isEqualTo(4);
		assertThat(ApiClient.<String>read(response, "$.rating.comment")).isEqualTo("Rápido y amable");
	}

	@Test
	void theRatingIsPartOfTheDetailForEveryoneWhoSeesTheRequest() throws Exception {
		String code = confirmed();
		ok(rate(STUDENT, code, "{\"score\":5}"));

		HttpResponse<String> detail = api.get("/api/v1/requests/" + code, api.login(SUPERVISOR));

		assertThat(ApiClient.<Integer>read(detail, "$.rating.score")).isEqualTo(5);
		assertThat(ApiClient.<Object>read(detail, "$.rating.comment")).isNull();
	}

	@Test
	void aRequestWithoutRatingHasANullRatingInTheDetail() throws Exception {
		String code = confirmed();

		HttpResponse<String> detail = api.get("/api/v1/requests/" + code, api.login(STUDENT));

		assertThat(ApiClient.<Object>read(detail, "$.rating")).isNull();
	}

	@Test
	void ratingDoesNotChangeTheStatusNorTheHistory() throws Exception {
		String code = confirmed();
		int historyBefore = jdbc.queryForObject("select count(*) from request_history h join service_requests r "
				+ "on r.id = h.request_id where r.code = ?", Integer.class, code);

		ok(rate(STUDENT, code, "{\"score\":3}"));

		assertThat(jdbc.queryForObject("select count(*) from request_history h join service_requests r "
				+ "on r.id = h.request_id where r.code = ?", Integer.class, code)).isEqualTo(historyBefore);
	}

	// ───── 400 ─────

	@Test
	void scoresOutsideOneToFiveAreRejected() throws Exception {
		String code = confirmed();

		for (String body : new String[] { "{\"score\":0}", "{\"score\":6}", "{}", "{\"comment\":\"ok\"}" }) {
			HttpResponse<String> response = rate(STUDENT, code, body);
			assertThat(response.statusCode()).as(body).isEqualTo(400);
			assertThat(ApiClient.<String>read(response, "$.errors.score")).isEqualTo("Califica de 1 a 5 estrellas");
		}
		assertThat(ratingsOf(code)).isZero();
	}

	@Test
	void aCommentLongerThan1000CharactersIsRejected() throws Exception {
		String code = confirmed();

		HttpResponse<String> response = rate(STUDENT, code,
				"{\"score\":4,\"comment\":\"" + "a".repeat(1001) + "\"}");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(ApiClient.<String>read(response, "$.errors.comment")).isNotBlank();
	}

	// ───── 401 / 404 ─────

	@Test
	void withoutTokenIs401() throws Exception {
		assertThat(rate(null, "SOL-2026-000001", "{\"score\":4}").statusCode()).isEqualTo(401);
	}

	@Test
	void anotherRequestersRequestIs404() throws Exception {
		String code = confirmed();

		assertThat(rate(OTHER_STUDENT, code, "{\"score\":4}").statusCode()).isEqualTo(404);
		assertThat(ratingsOf(code)).isZero();
	}

	@Test
	void staffWhoCanSeeTheRequestCannotRateIt() throws Exception {
		String code = confirmed();

		assertThat(rate(SUPERVISOR, code, "{\"score\":4}").statusCode()).isEqualTo(404);
		assertThat(rate(TECHNICIAN, code, "{\"score\":4}").statusCode()).isEqualTo(404);
	}

	@Test
	void anUnknownCodeIs404() throws Exception {
		assertThat(rate(STUDENT, "SOL-2026-999999", "{\"score\":4}").statusCode()).isEqualTo(404);
	}

	// ───── 409 ─────

	@Test
	void aResolvedRequestNotYetConfirmedIs409() throws Exception {
		String code = resolved();

		HttpResponse<String> response = rate(STUDENT, code, "{\"score\":4}");

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(ApiClient.<String>read(response, "$.detail"))
				.isEqualTo("Solo se puede calificar una solicitud cuya solución confirmaste");
	}

	@Test
	void aRequestClosedAsNotApplicableIs409() throws Exception {
		String code = ApiClient.read(api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}",
				api.login(STUDENT)), "$.code", String.class);
		ok(transition(SUPERVISOR, code, "{\"to\":\"CERRADA\",\"closureReason\":\"NO_PROCEDE\"}"));

		assertThat(rate(STUDENT, code, "{\"score\":4}").statusCode()).isEqualTo(409);
	}

	@Test
	void rateTwiceIs409AndKeepsTheFirstRating() throws Exception {
		String code = confirmed();
		ok(rate(STUDENT, code, "{\"score\":5}"));

		HttpResponse<String> second = rate(STUDENT, code, "{\"score\":1}");

		assertThat(second.statusCode()).isEqualTo(409);
		assertThat(ApiClient.<String>read(second, "$.detail")).isEqualTo("Ya calificaste esta solicitud");
		assertThat(ratingsOf(code)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select score from ratings", Integer.class)).isEqualTo(5);
	}

}
