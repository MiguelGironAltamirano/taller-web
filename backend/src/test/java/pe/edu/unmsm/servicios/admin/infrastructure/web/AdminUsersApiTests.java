package pe.edu.unmsm.servicios.admin.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class AdminUsersApiTests {

	private static final String DEMO_PASSWORD = "Demo12345!";
	private static final String ADMIN_EMAIL = "admin@unmsm.edu.pe";
	private static final long ADMIN_ID = 1L;
	private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
	private static final Pattern REFRESH_COOKIE = Pattern.compile("refresh_token=([^;]*)");
	private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
	private static final Pattern TEMPORARY_PASSWORD = Pattern.compile("\"temporaryPassword\":\"([^\"]+)\"");

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> send(String method, String path, String json, String accessToken) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.method(method, json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
		if (accessToken != null) {
			builder.header("Authorization", "Bearer " + accessToken);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> login(String email, String password) throws Exception {
		return send("POST", "/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}",
				null);
	}

	private HttpResponse<String> refresh(String cookie) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/refresh"))
				.header("Cookie", "refresh_token=" + cookie).POST(BodyPublishers.noBody()).build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private String tokenOf(String email) throws Exception {
		HttpResponse<String> response = login(email, DEMO_PASSWORD);
		assertThat(response.statusCode()).isEqualTo(200);
		return group(ACCESS_TOKEN, response.body());
	}

	private String adminToken() throws Exception {
		return tokenOf(ADMIN_EMAIL);
	}

	private static String group(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text);
		assertThat(matcher.find()).as("pattern %s in %s", pattern, text).isTrue();
		return matcher.group(1);
	}

	private static String uniqueEmail() {
		return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@unmsm.edu.pe";
	}

	private static String userBody(String fullName, String email, String role, Long areaId) {
		return "{\"fullName\":\"" + fullName + "\",\"email\":\"" + email + "\",\"role\":\"" + role + "\",\"areaId\":"
				+ areaId + "}";
	}

	private HttpResponse<String> createTechnician(String token, String email) throws Exception {
		return send("POST", "/api/v1/admin/users", userBody("Carlos Técnico", email, "TECNICO", 1L), token);
	}

	@Test
	void onlyAdministratorsCanUseTheAdminUserEndpoints() throws Exception {
		for (String email : new String[] { "estudiante@unmsm.edu.pe", "supervisor.mantenimiento@unmsm.edu.pe",
				"tecnico.mantenimiento@unmsm.edu.pe" }) {
			HttpResponse<String> response = send("GET", "/api/v1/admin/users", null, tokenOf(email));
			assertThat(response.statusCode()).as(email).isEqualTo(403);
			assertThat(response.headers().firstValue("Content-Type"))
					.hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
		}
		assertThat(send("GET", "/api/v1/admin/users", null, null).statusCode()).isEqualTo(401);
		assertThat(send("GET", "/api/v1/admin/states", null, tokenOf("estudiante@unmsm.edu.pe")).statusCode())
				.isEqualTo(403);
	}

	@Test
	void listsUsersWithTheirAreaNameAndNoPasswords() throws Exception {
		HttpResponse<String> response = send("GET", "/api/v1/admin/users", null, adminToken());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"email\":\"supervisor.mantenimiento@unmsm.edu.pe\"")
				.contains("\"areaName\":\"Mantenimiento\"").contains("\"requesterType\":\"ESTUDIANTE\"")
				.contains("\"active\":true").doesNotContain("password").doesNotContain("$2");
	}

	@Test
	void createdTechnicianCanLogInWithTheTemporaryPassword() throws Exception {
		String email = uniqueEmail();
		HttpResponse<String> created = createTechnician(adminToken(), email);

		assertThat(created.statusCode()).isEqualTo(201);
		assertThat(created.body()).contains("\"role\":\"TECNICO\"").contains("\"areaId\":1")
				.contains("\"areaName\":\"Mantenimiento\"").contains("\"active\":true");
		String temporaryPassword = group(TEMPORARY_PASSWORD, created.body());
		String storedHash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);
		assertThat(storedHash).startsWith("$2").isNotEqualTo(temporaryPassword);

		HttpResponse<String> login = login(email, temporaryPassword);
		assertThat(login.statusCode()).isEqualTo(200);
		assertThat(login.body()).contains("\"role\":\"TECNICO\"").contains("\"areaId\":1");
	}

	@Test
	void temporaryPasswordIsNotReturnedOutsideTheCreation() throws Exception {
		String token = adminToken();
		String email = uniqueEmail();
		String id = group(ID, createTechnician(token, email).body());

		assertThat(send("GET", "/api/v1/admin/users", null, token).body()).doesNotContain("temporaryPassword");
		assertThat(send("PUT", "/api/v1/admin/users/" + id, userBody("Carlos Técnico", email, "TECNICO", 1L), token)
			.body()).doesNotContain("temporaryPassword");
	}

	@Test
	void creationValidatesAreaAndEmail() throws Exception {
		String token = adminToken();

		HttpResponse<String> withoutArea = send("POST", "/api/v1/admin/users",
				userBody("Carlos Técnico", uniqueEmail(), "TECNICO", null), token);
		assertThat(withoutArea.statusCode()).isEqualTo(400);
		assertThat(withoutArea.body()).contains("\"errors\":{").contains("\"areaId\":");

		HttpResponse<String> unknownArea = send("POST", "/api/v1/admin/users",
				userBody("Carlos Técnico", uniqueEmail(), "TECNICO", 999L), token);
		assertThat(unknownArea.statusCode()).isEqualTo(400);
		assertThat(unknownArea.body()).contains("\"areaId\":");

		HttpResponse<String> requesterWithArea = send("POST", "/api/v1/admin/users",
				userBody("Ana Pérez", uniqueEmail(), "SOLICITANTE", 1L), token);
		assertThat(requesterWithArea.statusCode()).isEqualTo(400);
		assertThat(requesterWithArea.body()).contains("\"areaId\":");

		HttpResponse<String> foreignEmail = send("POST", "/api/v1/admin/users",
				userBody("Carlos Técnico", "carlos@gmail.com", "TECNICO", 1L), token);
		assertThat(foreignEmail.statusCode()).isEqualTo(400);
		assertThat(foreignEmail.body()).contains("\"email\":");
	}

	@Test
	void repeatedEmailIsAConflict() throws Exception {
		HttpResponse<String> response = createTechnician(adminToken(), "Admin@UNMSM.edu.pe");

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.body()).contains("\"email\":");
	}

	@Test
	void updateChangesRoleAndArea() throws Exception {
		String token = adminToken();
		String email = uniqueEmail();
		String id = group(ID, createTechnician(token, email).body());

		HttpResponse<String> response = send("PUT", "/api/v1/admin/users/" + id,
				userBody("Carla Supervisora", email, "SUPERVISOR", 2L), token);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"fullName\":\"Carla Supervisora\"").contains("\"role\":\"SUPERVISOR\"")
				.contains("\"areaName\":\"Infraestructura\"");
	}

	@Test
	void updatingToAnotherUsersEmailIsAConflict() throws Exception {
		String token = adminToken();
		String id = group(ID, createTechnician(token, uniqueEmail()).body());

		HttpResponse<String> response = send("PUT", "/api/v1/admin/users/" + id,
				userBody("Carlos Técnico", ADMIN_EMAIL, "TECNICO", 1L), token);

		assertThat(response.statusCode()).isEqualTo(409);
	}

	@Test
	void missingUsersAreNotFound() throws Exception {
		String token = adminToken();

		assertThat(send("PUT", "/api/v1/admin/users/999999", userBody("Nadie", uniqueEmail(), "TECNICO", 1L), token)
			.statusCode()).isEqualTo(404);
		assertThat(send("PATCH", "/api/v1/admin/users/999999/active", "{\"active\":false}", token).statusCode())
			.isEqualTo(404);
	}

	@Test
	void deactivationBlocksLoginAndEndsSessionsUntilReactivated() throws Exception {
		String token = adminToken();
		String email = uniqueEmail();
		HttpResponse<String> created = createTechnician(token, email);
		String id = group(ID, created.body());
		String password = group(TEMPORARY_PASSWORD, created.body());
		String cookie = group(REFRESH_COOKIE,
				login(email, password).headers().allValues("Set-Cookie").stream()
					.filter(value -> value.startsWith("refresh_token=")).findFirst().orElseThrow());

		HttpResponse<String> deactivated = send("PATCH", "/api/v1/admin/users/" + id + "/active", "{\"active\":false}",
				token);

		assertThat(deactivated.statusCode()).isEqualTo(200);
		assertThat(deactivated.body()).contains("\"active\":false");
		assertThat(login(email, password).statusCode()).isEqualTo(401);
		assertThat(refresh(cookie).statusCode()).isEqualTo(401);
		Integer activeTokens = jdbc.queryForObject(
				"select count(*) from refresh_tokens where user_id = ? and revoked_at is null", Integer.class,
				Long.parseLong(id));
		assertThat(activeTokens).isZero();

		HttpResponse<String> reactivated = send("PATCH", "/api/v1/admin/users/" + id + "/active", "{\"active\":true}",
				token);
		assertThat(reactivated.statusCode()).isEqualTo(200);
		assertThat(login(email, password).statusCode()).isEqualTo(200);
	}

	@Test
	void administratorCannotDeactivateThemselvesOrChangeTheirOwnRole() throws Exception {
		String token = adminToken();

		HttpResponse<String> deactivate = send("PATCH", "/api/v1/admin/users/" + ADMIN_ID + "/active",
				"{\"active\":false}", token);
		HttpResponse<String> demote = send("PUT", "/api/v1/admin/users/" + ADMIN_ID,
				userBody("Administrador Demo", ADMIN_EMAIL, "SOLICITANTE", null), token);

		assertThat(deactivate.statusCode()).isEqualTo(409);
		assertThat(demote.statusCode()).isEqualTo(409);
		assertThat(jdbc.queryForObject("select role from users where id = ?", String.class, ADMIN_ID))
				.isEqualTo("ADMINISTRADOR");
		assertThat(jdbc.queryForObject("select active from users where id = ?", Boolean.class, ADMIN_ID)).isTrue();
	}

	@Test
	void statesAreListedInWorkflowOrder() throws Exception {
		HttpResponse<String> response = send("GET", "/api/v1/admin/states", null, adminToken());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).containsSubsequence("\"REGISTRADA\"", "\"EN_EVALUACION\"", "\"ASIGNADA\"",
				"\"EN_ATENCION\"", "\"RESUELTA\"", "\"CERRADA\"");
		assertThat(response.body()).contains("\"order\":1").contains("\"order\":6");
	}

}
