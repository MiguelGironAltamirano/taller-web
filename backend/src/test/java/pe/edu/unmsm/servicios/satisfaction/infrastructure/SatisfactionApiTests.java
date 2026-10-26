package pe.edu.unmsm.servicios.satisfaction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Instant;
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
import pe.edu.unmsm.servicios.satisfaction.application.RatingQueries;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

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

	@Autowired
	RatingQueries ratings;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		jdbc.update("delete from request_code_sequences");
		jdbc.update("insert ignore into users (full_name, email, password_hash, role, requester_type, active, "
				+ "created_at) values ('Otro Calificador', ?, ?, 'SOLICITANTE', 'DOCENTE', true, utc_timestamp(6))",
				OTHER_STUDENT, DEMO_HASH);
	}

	private String createRequest() throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}",
				api.login(STUDENT));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return ApiClient.read(response, "$.code", String.class);
	}

	/** A request resolved by the technician and closed by the requester's confirmation, through the API. */
	private String confirmedRequest() throws Exception {
		String code = createRequest();
		jdbc.update("update service_requests set status = 'EN_ATENCION', technician_id = 3 where code = ?", code);
		assertThat(api.post("/api/v1/requests/" + code + "/transitions",
				"{\"to\":\"RESUELTA\",\"reason\":\"Se cambió el tomacorriente\"}", api.login(TECHNICIAN)).statusCode())
				.isEqualTo(200);
		assertThat(api.post("/api/v1/requests/" + code + "/transitions", "{\"to\":\"CERRADA\"}", api.login(STUDENT))
				.statusCode()).isEqualTo(200);
		return code;
	}

	private HttpResponse<String> rate(String user, String code, String json) throws Exception {
		return api.post("/api/v1/requests/" + code + "/rating", json, api.login(user));
	}

	@Test
	void theRequesterRatesAConfirmedRequestAndTheDetailShowsIt() throws Exception {
		String code = confirmedRequest();

		HttpResponse<String> response = rate(STUDENT, code, "{\"score\":4,\"comment\":\"  Muy rápido  \"}");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.<Integer>read(response, "$.rating.score")).isEqualTo(4);
		assertThat(ApiClient.<String>read(response, "$.rating.comment")).isEqualTo("Muy rápido");
		assertThat(ApiClient.<String>read(response, "$.status")).as("rating does not change the status")
				.isEqualTo("CERRADA");
		assertThat(ApiClient.<String>read(response, "$.solution")).isEqualTo("Se cambió el tomacorriente");
		HttpResponse<String> seenBySupervisor = api.get("/api/v1/requests/" + code, api.login(SUPERVISOR));
		assertThat(ApiClient.<Integer>read(seenBySupervisor, "$.rating.score")).isEqualTo(4);
		assertThat(ApiClient.<List<String>>read(seenBySupervisor, "$.history[*].toStatus")).hasSize(3);
	}

	@Test
	void theCommentIsOptional() throws Exception {
		String code = confirmedRequest();

		HttpResponse<String> response = rate(STUDENT, code, "{\"score\":5}");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.<Object>read(response, "$.rating.comment")).isNull();
	}

	@Test
	void anUnratedRequestHasNoRating() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/requests/" + createRequest(), api.login(STUDENT));

		assertThat(ApiClient.<Object>read(response, "$.rating")).isNull();
	}

	@Test
	void aRequestCanBeRatedOnlyOnce() throws Exception {
		String code = confirmedRequest();
		assertThat(rate(STUDENT, code, "{\"score\":4}").statusCode()).isEqualTo(200);

		ApiClient.assertProblem(rate(STUDENT, code, "{\"score\":1}"), 409);
		assertThat(jdbc.queryForObject("select score from request_ratings", Integer.class)).isEqualTo(4);
	}

	@Test
	void onlyARequestClosedByConfirmationCanBeRated() throws Exception {
		String open = createRequest();
		ApiClient.assertProblem(rate(STUDENT, open, "{\"score\":4}"), 409);

		String unresolved = createRequest();
		jdbc.update("update service_requests set status = 'CERRADA', closure_reason = 'NO_PROCEDE' where code = ?",
				unresolved);
		ApiClient.assertProblem(rate(STUDENT, unresolved, "{\"score\":4}"), 409);

		String automatic = createRequest();
		jdbc.update("update service_requests set status = 'CERRADA', closure_reason = 'AUTOMATICA' where code = ?",
				automatic);
		ApiClient.assertProblem(rate(STUDENT, automatic, "{\"score\":4}"), 409);
	}

	@Test
	void anInvalidScoreOrCommentIsRejectedWithTheFieldError() throws Exception {
		String code = confirmedRequest();

		for (String body : List.of("{}", "{\"score\":0}", "{\"score\":6}")) {
			HttpResponse<String> response = rate(STUDENT, code, body);
			ApiClient.assertProblem(response, 400);
			assertThat(ApiClient.<String>read(response, "$.errors.score")).as(body).isNotBlank();
		}
		HttpResponse<String> longComment = rate(STUDENT, code,
				"{\"score\":3,\"comment\":\"" + "x".repeat(501) + "\"}");
		ApiClient.assertProblem(longComment, 400);
		assertThat(ApiClient.<String>read(longComment, "$.errors.comment")).isNotBlank();
	}

	@Test
	void onlyTheOwningRequesterMayRate() throws Exception {
		String code = confirmedRequest();

		ApiClient.assertProblem(rate(SUPERVISOR, code, "{\"score\":4}"), 403);
		ApiClient.assertProblem(rate(TECHNICIAN, code, "{\"score\":4}"), 403);
		ApiClient.assertProblem(rate(OTHER_STUDENT, code, "{\"score\":4}"), 404);
		ApiClient.assertProblem(rate(STUDENT, "SOL-2026-999999", "{\"score\":4}"), 404);
		assertThat(jdbc.queryForObject("select count(*) from request_ratings", Integer.class)).isZero();
	}

	@Test
	void theReadPortListsTheRatingsOfAPeriod() throws Exception {
		String code = confirmedRequest();
		Instant before = Instant.now().minusSeconds(60);
		rate(STUDENT, code, "{\"score\":2}");

		assertThat(ratings.findCreatedBetween(before, Instant.now().plusSeconds(60)))
				.singleElement().satisfies(rating -> assertThat(rating.score()).isEqualTo(2));
		assertThat(ratings.findCreatedBetween(before.minusSeconds(3600), before)).isEmpty();
	}

}
