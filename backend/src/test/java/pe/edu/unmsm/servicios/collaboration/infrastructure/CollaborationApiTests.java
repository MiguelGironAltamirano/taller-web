package pe.edu.unmsm.servicios.collaboration.infrastructure;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class CollaborationApiTests {

	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String SUPERVISOR_AREA1 = "supervisor.mantenimiento@unmsm.edu.pe";
	private static final String OTHER_STUDENT = "otra.solicitante@unmsm.edu.pe";
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("""
				INSERT INTO users (full_name, email, password_hash, role, requester_type, area_id, active, created_at)
				VALUES ('Otra Solicitante', ?, ?, 'SOLICITANTE', 'ESTUDIANTE', NULL, TRUE, NOW(6))
				ON DUPLICATE KEY UPDATE active = TRUE
				""", OTHER_STUDENT, DEMO_HASH);
	}

	private String createRequestAs(String email) throws Exception {
		String token = api.login(email);
		String body = """
				{
					"categoryId": 1,
					"locationId": 3,
					"priorityId": 1,
					"description": "Fallo en el tomacorriente del aula 101"
				}
				""";
		HttpResponse<String> res = api.post("/api/v1/requests", body, token);
		assertThat(res.statusCode()).isEqualTo(201);
		return ApiClient.read(res, "$.code", String.class);
	}

	@Test
	void addCommentToVisibleRequestReturns201() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		String body = """
				{
					"text": "Se necesita urgente porque hay clases."
				}
				""";

		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/comments", body, token);
		assertThat(response.statusCode()).isEqualTo(201);
		assertThat(ApiClient.read(response, "$.authorName", String.class)).isEqualTo("Estudiante Demo");
		assertThat(ApiClient.read(response, "$.text", String.class)).isEqualTo("Se necesita urgente porque hay clases.");
	}

	@Test
	void listCommentsReturnsChronologicalList() throws Exception {
		String code = createRequestAs(STUDENT);
		String studentToken = api.login(STUDENT);
		String supervisorToken = api.login(SUPERVISOR_AREA1);

		api.post("/api/v1/requests/" + code + "/comments", "{\"text\": \"Primer comentario\"}", studentToken);
		api.post("/api/v1/requests/" + code + "/comments", "{\"text\": \"Segundo comentario\"}", supervisorToken);

		HttpResponse<String> response = api.get("/api/v1/requests/" + code + "/comments", studentToken);
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Primer comentario").contains("Segundo comentario");
	}

	@Test
	void addCommentToInvisibleRequestReturns404() throws Exception {
		String code = createRequestAs(STUDENT);
		String otherToken = api.login(OTHER_STUDENT);
		String body = """
				{
					"text": "Intento de comentar solicitud ajena"
				}
				""";

		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/comments", body, otherToken);
		assertThat(response.statusCode()).isEqualTo(404);
	}

	@Test
	void emptyCommentIsRejectedWith400() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		String body = """
				{
					"text": "   "
				}
				""";

		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/comments", body, token);
		assertThat(response.statusCode()).isEqualTo(400);
	}

	@Test
	void commentOver1000CharsIsRejectedWith400() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		String body = "{\"text\": \"" + "x".repeat(1001) + "\"}";

		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/comments", body, token);
		assertThat(response.statusCode()).isEqualTo(400);
	}

	@Test
	void addCommentWithoutTokenReturns401() throws Exception {
		String code = createRequestAs(STUDENT);
		String body = "{\"text\": \"Sin autenticacion\"}";

		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/comments", body, null);
		assertThat(response.statusCode()).isEqualTo(401);
	}

}
