package pe.edu.unmsm.servicios.notifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/**
 * Authenticates exactly like the real clients (login, then the access token as a Bearer header) so the
 * controller receives the JWT principal produced by the resource server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class NotificationsApiTests {

	// Seeded by seed-dev.
	private static final String STUDENT = "estudiante@unmsm.edu.pe"; // id 4
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // id 2, area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	private static final String VALID = "El tomacorriente del aula no funciona desde ayer";

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcTemplate jdbc;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from notifications");
	}

	private String createRequest(String user) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests",
				"{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2}", api.login(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return ApiClient.read(response, "$.code", String.class);
	}

	private void transition(String user, String code, String json) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/requests/" + code + "/transitions", json, api.login(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
	}

	private HttpResponse<String> list(String token) throws Exception {
		HttpResponse<String> response = api.get("/api/v1/notifications", token);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		return response;
	}

	@Test
	@DisplayName("List notifications without token returns 401")
	void listWithoutTokenReturns401() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/notifications", null);
		assertThat(response.statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("A transition notifies the requester with the expected body and skips the actor")
	void transitionNotifiesRequesterAndSkipsActor() throws Exception {
		String code = createRequest(STUDENT);
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");

		HttpResponse<String> student = list(api.login(STUDENT));
		assertThat(ApiClient.<List<String>>read(student, "$[*].text"))
				.containsExactly("La solicitud " + code + " pasó a En evaluación.");
		assertThat(ApiClient.<String>read(student, "$[0].requestCode")).isEqualTo(code);
		assertThat(ApiClient.<Boolean>read(student, "$[0].read")).isFalse();
		assertThat(ApiClient.<Integer>read(student, "$[0].id")).isPositive();
		assertThat(ApiClient.<String>read(student, "$[0].createdAt")).isNotBlank();

		HttpResponse<String> supervisor = list(api.login(SUPERVISOR));
		assertThat(ApiClient.<List<String>>read(supervisor, "$[*].requestCode")).doesNotContain(code);
	}

	@Test
	@DisplayName("Assignment notifies the technician once; the list holds only the user's own items, newest first")
	void assignmentNotifiesTechnicianNewestFirst() throws Exception {
		String code = createRequest(STUDENT);
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");
		transition(SUPERVISOR, code, "{\"to\":\"ASIGNADA\",\"technicianId\":3}");

		HttpResponse<String> technician = list(api.login(TECHNICIAN));
		assertThat(ApiClient.<List<String>>read(technician, "$[*].text"))
				.containsExactly("Se te asignó la solicitud " + code + ".");

		HttpResponse<String> student = list(api.login(STUDENT));
		assertThat(ApiClient.<List<String>>read(student, "$[*].text")).containsExactly(
				"La solicitud " + code + " pasó a Asignada.", "La solicitud " + code + " pasó a En evaluación.");
	}

	@Test
	@DisplayName("Mark read returns 204 for the owner and 404 for anybody else")
	void markReadOwnAndForeign() throws Exception {
		String code = createRequest(STUDENT);
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");
		String studentToken = api.login(STUDENT);
		int id = ApiClient.read(list(studentToken), "$[0].id", Integer.class);

		HttpResponse<String> foreign = api.send("PATCH", "/api/v1/notifications/" + id + "/read", null,
				api.login(SUPERVISOR));
		ApiClient.assertProblem(foreign, 404);
		assertThat(ApiClient.<Boolean>read(list(studentToken), "$[0].read")).isFalse();

		HttpResponse<String> own = api.send("PATCH", "/api/v1/notifications/" + id + "/read", null, studentToken);
		assertThat(own.statusCode()).as(own.body()).isEqualTo(204);
		assertThat(ApiClient.<Boolean>read(list(studentToken), "$[0].read")).isTrue();
	}

	@Test
	@DisplayName("Read-all returns 204 and only affects the caller's notifications")
	void markAllReadOnlyAffectsOwnNotifications() throws Exception {
		String code = createRequest(STUDENT);
		transition(SUPERVISOR, code, "{\"to\":\"EN_EVALUACION\"}");
		transition(SUPERVISOR, code, "{\"to\":\"ASIGNADA\",\"technicianId\":3}");
		String studentToken = api.login(STUDENT);

		HttpResponse<String> response = api.post("/api/v1/notifications/read-all", "", studentToken);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(204);

		assertThat(ApiClient.<List<Boolean>>read(list(studentToken), "$[*].read")).hasSize(2).containsOnly(true);
		assertThat(ApiClient.<List<Boolean>>read(list(api.login(TECHNICIAN)), "$[*].read")).hasSize(1)
				.containsOnly(false);
	}

}
