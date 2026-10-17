package pe.edu.unmsm.servicios.auth.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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
class AuthApiTests {

	private static final String DEMO_EMAIL = "estudiante@unmsm.edu.pe";
	private static final String DEMO_PASSWORD = "Demo12345!";
	private static final Pattern COOKIE_VALUE = Pattern.compile("refresh_token=([^;]*)");
	private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> post(String path, String json, String refreshCookie) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.POST(json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
		if (refreshCookie != null) {
			builder.header("Cookie", "refresh_token=" + refreshCookie);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String path, String accessToken) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Authorization", "Bearer " + accessToken).GET().build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static String registerBody(String email, String password) {
		return "{\"fullName\":\"Ana Pérez\",\"email\":\"" + email + "\",\"password\":\"" + password
				+ "\",\"requesterType\":\"ESTUDIANTE\"}";
	}

	private static String loginBody(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static String uniqueEmail() {
		return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@unmsm.edu.pe";
	}

	private static String refreshCookieOf(HttpResponse<?> response) {
		String header = response.headers().allValues("Set-Cookie").stream()
				.filter(value -> value.startsWith("refresh_token=")).findFirst().orElseThrow();
		Matcher matcher = COOKIE_VALUE.matcher(header);
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private static String accessTokenOf(HttpResponse<String> response) {
		Matcher matcher = ACCESS_TOKEN.matcher(response.body());
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private static String sha256(String value) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
	}

	private HttpResponse<String> loginDemo() throws Exception {
		return post("/api/v1/auth/login", loginBody(DEMO_EMAIL, DEMO_PASSWORD), null);
	}

	@Test
	void registerCreatesRequesterWithHashedPassword() throws Exception {
		String email = uniqueEmail();
		HttpResponse<String> response = post("/api/v1/auth/register", registerBody(email, "Clave12345"), null);

		assertThat(response.statusCode()).isEqualTo(201);
		assertThat(response.body()).contains("\"email\":\"" + email + "\"").contains("\"role\":\"SOLICITANTE\"")
				.contains("\"areaId\":null").doesNotContain("password");
		String stored = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);
		String role = jdbc.queryForObject("select role from users where email = ?", String.class, email);
		assertThat(stored).startsWith("$2").isNotEqualTo("Clave12345");
		assertThat(role).isEqualTo("SOLICITANTE");
	}

	@Test
	void registerRejectsNonUnmsmEmail() throws Exception {
		HttpResponse<String> response = post("/api/v1/auth/register", registerBody("ana@gmail.com", "Clave12345"), null);

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
		assertThat(response.body()).contains("\"errors\":{").contains("\"email\":");
	}

	@Test
	void registerRejectsWeakPassword() throws Exception {
		HttpResponse<String> response = post("/api/v1/auth/register", registerBody(uniqueEmail(), "sololetras"), null);

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"password\":");
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		String email = uniqueEmail();
		assertThat(post("/api/v1/auth/register", registerBody(email, "Clave12345"), null).statusCode()).isEqualTo(201);

		HttpResponse<String> response = post("/api/v1/auth/register", registerBody(email, "Clave12345"), null);

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

	@Test
	void loginReturnsWorkingAccessTokenAndRefreshCookie() throws Exception {
		HttpResponse<String> response = loginDemo();

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"expiresIn\":900").contains("\"role\":\"SOLICITANTE\"");
		HttpResponse<String> me = get("/api/v1/me", accessTokenOf(response));
		assertThat(me.statusCode()).isEqualTo(200);
		assertThat(me.body()).contains("\"email\":\"" + DEMO_EMAIL + "\"");

		String cookie = response.headers().allValues("Set-Cookie").stream()
				.filter(value -> value.startsWith("refresh_token=")).findFirst().orElseThrow();
		assertThat(cookie).contains("HttpOnly").contains("Secure").contains("SameSite=Strict")
				.contains("Path=/api/v1/auth").contains("Max-Age=604800");
		String stored = jdbc.queryForObject("select token_hash from refresh_tokens where token_hash = ?", String.class,
				sha256(refreshCookieOf(response)));
		assertThat(stored).hasSize(64);
	}

	@Test
	void supervisorLoginCarriesTheAreaInTheUserAndInMe() throws Exception {
		HttpResponse<String> response = post("/api/v1/auth/login",
				loginBody("supervisor.mantenimiento@unmsm.edu.pe", DEMO_PASSWORD), null);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"areaId\":1");
		assertThat(get("/api/v1/me", accessTokenOf(response)).body()).contains("\"areaId\":1");
	}

	@Test
	void wrongPasswordAndUnknownEmailReturnTheSameProblem() throws Exception {
		HttpResponse<String> wrongPassword = post("/api/v1/auth/login", loginBody(DEMO_EMAIL, "Incorrecta1"), null);
		HttpResponse<String> unknownEmail = post("/api/v1/auth/login", loginBody("nadie@unmsm.edu.pe", "Incorrecta1"), null);

		assertThat(wrongPassword.statusCode()).isEqualTo(401);
		assertThat(unknownEmail.statusCode()).isEqualTo(401);
		assertThat(wrongPassword.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
		assertThat(wrongPassword.body()).isEqualTo(unknownEmail.body());
	}

	@Test
	void inactiveUserCannotLogin() throws Exception {
		String email = uniqueEmail();
		post("/api/v1/auth/register", registerBody(email, "Clave12345"), null);
		jdbc.update("update users set active = false where email = ?", email);

		assertThat(post("/api/v1/auth/login", loginBody(email, "Clave12345"), null).statusCode()).isEqualTo(401);
	}

	@Test
	void refreshRotatesTheTokenAndRejectsReuse() throws Exception {
		String oldToken = refreshCookieOf(loginDemo());

		HttpResponse<String> refreshed = post("/api/v1/auth/refresh", null, oldToken);

		assertThat(refreshed.statusCode()).isEqualTo(200);
		String newToken = refreshCookieOf(refreshed);
		assertThat(newToken).isNotEqualTo(oldToken);
		assertThat(get("/api/v1/me", accessTokenOf(refreshed)).statusCode()).isEqualTo(200);
		Object revokedAt = jdbc.queryForObject("select revoked_at from refresh_tokens where token_hash = ?",
				Object.class, sha256(oldToken));
		assertThat(revokedAt).isNotNull();
		assertThat(post("/api/v1/auth/refresh", null, oldToken).statusCode()).isEqualTo(401);
		assertThat(post("/api/v1/auth/refresh", null, newToken).statusCode()).isEqualTo(200);
	}

	@Test
	void refreshWithoutOrWithUnknownCookieIsUnauthorized() throws Exception {
		assertThat(post("/api/v1/auth/refresh", null, null).statusCode()).isEqualTo(401);
		assertThat(post("/api/v1/auth/refresh", null, "desconocido").statusCode()).isEqualTo(401);
	}

	@Test
	void refreshRejectsExpiredToken() throws Exception {
		String token = refreshCookieOf(loginDemo());
		jdbc.update("update refresh_tokens set expires_at = ? where token_hash = ?",
				java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)), sha256(token));

		assertThat(post("/api/v1/auth/refresh", null, token).statusCode()).isEqualTo(401);
	}

	@Test
	void logoutRevokesTheTokenAndClearsTheCookie() throws Exception {
		String token = refreshCookieOf(loginDemo());

		HttpResponse<String> response = post("/api/v1/auth/logout", null, token);

		assertThat(response.statusCode()).isEqualTo(204);
		String cleared = response.headers().allValues("Set-Cookie").stream()
				.filter(value -> value.startsWith("refresh_token=")).findFirst().orElseThrow();
		assertThat(cleared).contains("Max-Age=0").contains("Path=/api/v1/auth");
		Object revokedAt = jdbc.queryForObject("select revoked_at from refresh_tokens where token_hash = ?",
				Object.class, sha256(token));
		assertThat(revokedAt).isNotNull();
		assertThat(post("/api/v1/auth/refresh", null, token).statusCode()).isEqualTo(401);
	}

	@Test
	void logoutWithoutCookieStillSucceeds() throws Exception {
		assertThat(post("/api/v1/auth/logout", null, null).statusCode()).isEqualTo(204);
	}

	private HttpResponse<String> put(String path, String json, String accessToken) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.PUT(json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
		if (accessToken != null) {
			builder.header("Authorization", "Bearer " + accessToken);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void changePasswordWithCorrectCurrentPasswordReturns204AndRevokesRefreshTokens() throws Exception {
		HttpResponse<String> loginRes = loginDemo();
		String accessToken = accessTokenOf(loginRes);
		String refreshToken = refreshCookieOf(loginRes);

		String body = """
				{
					"currentPassword": "%s",
					"newPassword": "NewPassword123!"
				}
				""".formatted(DEMO_PASSWORD);

		HttpResponse<String> response = put("/api/v1/auth/password", body, accessToken);
		assertThat(response.statusCode()).isEqualTo(204);

		assertThat(post("/api/v1/auth/refresh", null, refreshToken).statusCode()).isEqualTo(401);

		String oldLoginBody = """
				{
					"email": "%s",
					"password": "%s"
				}
				""".formatted(DEMO_EMAIL, DEMO_PASSWORD);
		assertThat(post("/api/v1/auth/login", oldLoginBody, null).statusCode()).isEqualTo(401);

		String newLoginBody = """
				{
					"email": "%s",
					"password": "NewPassword123!"
				}
				""".formatted(DEMO_EMAIL);
		HttpResponse<String> newLoginRes = post("/api/v1/auth/login", newLoginBody, null);
		assertThat(newLoginRes.statusCode()).isEqualTo(200);

		String newAccessToken = accessTokenOf(newLoginRes);
		String restoreBody = """
				{
					"currentPassword": "NewPassword123!",
					"newPassword": "%s"
				}
				""".formatted(DEMO_PASSWORD);
		assertThat(put("/api/v1/auth/password", restoreBody, newAccessToken).statusCode()).isEqualTo(204);
	}

	@Test
	void changePasswordWithWrongCurrentPasswordReturns400() throws Exception {
		String accessToken = accessTokenOf(loginDemo());
		String body = """
				{
					"currentPassword": "WrongPassword123!",
					"newPassword": "NewPassword123!"
				}
				""";
		HttpResponse<String> response = put("/api/v1/auth/password", body, accessToken);
		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("currentPassword");
	}

	@Test
	void changePasswordWithInvalidNewPasswordReturns400() throws Exception {
		String accessToken = accessTokenOf(loginDemo());
		String body = """
				{
					"currentPassword": "%s",
					"newPassword": "short"
				}
				""".formatted(DEMO_PASSWORD);
		HttpResponse<String> response = put("/api/v1/auth/password", body, accessToken);
		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("newPassword");
	}

	@Test
	void changePasswordWithoutTokenReturns401() throws Exception {
		String body = """
				{
					"currentPassword": "%s",
					"newPassword": "NewPassword123!"
				}
				""".formatted(DEMO_PASSWORD);
		HttpResponse<String> response = put("/api/v1/auth/password", body, null);
		assertThat(response.statusCode()).isEqualTo(401);
	}

}
