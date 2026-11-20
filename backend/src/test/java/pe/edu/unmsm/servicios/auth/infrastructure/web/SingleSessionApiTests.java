package pe.edu.unmsm.servicios.auth.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.AccessTokenIssuer;
import pe.edu.unmsm.servicios.testsupport.ApiClient;
import pe.edu.unmsm.servicios.testsupport.TestPasskeyAuthenticator;

/** One open session per user, docs/api/contrato-v1.md «Una sola sesión por usuario». */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class SingleSessionApiTests {

	private static final String PASSWORD = "Clave12345";
	private static final Pattern COOKIE_VALUE = Pattern.compile("refresh_token=([^;]*)");
	private static final String REPLACED = "Tu sesión se cerró porque ingresaste desde otro dispositivo";

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	AccessTokenIssuer issuer;

	private final HttpClient http = HttpClient.newHttpClient();
	private String email;

	/** A fresh account per test, so no other test ever opens or closes its sessions. */
	@BeforeEach
	void setUp() throws Exception {
		email = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@unmsm.edu.pe";
		HttpResponse<String> registered = send("POST", "/api/v1/auth/register", "{\"fullName\":\"Pepe Sesión\",\"email\":\""
				+ email + "\",\"password\":\"" + PASSWORD + "\",\"requesterType\":\"ESTUDIANTE\"}", null, null);
		assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
	}

	private record Session(String accessToken, String refreshToken) {
	}

	private HttpResponse<String> send(String method, String path, String json, String bearer, String refreshCookie)
			throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.method(method, json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
		if (bearer != null) {
			builder.header("Authorization", "Bearer " + bearer);
		}
		if (refreshCookie != null) {
			builder.header("Cookie", "refresh_token=" + refreshCookie);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> loginResponse(String bearer) throws Exception {
		return send("POST", "/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
				bearer, null);
	}

	private Session login() throws Exception {
		return sessionOf(loginResponse(null));
	}

	private static Session sessionOf(HttpResponse<String> response) {
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		String header = response.headers().allValues("Set-Cookie").stream()
				.filter(value -> value.startsWith("refresh_token=")).findFirst().orElseThrow();
		Matcher matcher = COOKIE_VALUE.matcher(header);
		assertThat(matcher.find()).isTrue();
		return new Session(ApiClient.read(response, "$.accessToken", String.class), matcher.group(1));
	}

	private HttpResponse<String> me(String accessToken) throws Exception {
		return send("GET", "/api/v1/me", null, accessToken, null);
	}

	private HttpResponse<String> refresh(String refreshToken) throws Exception {
		return send("POST", "/api/v1/auth/refresh", null, null, refreshToken);
	}

	private static String sessionIdOf(String accessToken) {
		String payload = new String(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]), StandardCharsets.UTF_8);
		Matcher matcher = Pattern.compile("\"sid\":\"([^\"]+)\"").matcher(payload);
		assertThat(matcher.find()).as(payload).isTrue();
		return matcher.group(1);
	}

	private int openSessions() {
		return jdbc.queryForObject("select count(distinct t.session_id) from refresh_tokens t join users u on "
				+ "u.id = t.user_id where u.email = ? and t.revoked_at is null and t.expires_at > now(6)",
				Integer.class, email);
	}

	private static void assertReplaced(HttpResponse<String> response) {
		ApiClient.assertProblem(response, 401);
		assertThat(ApiClient.<String>read(response, "$.code")).isEqualTo("SESSION_REPLACED");
		assertThat(ApiClient.<String>read(response, "$.detail")).isEqualTo(REPLACED);
	}

	private static void assertEndedWithoutReason(HttpResponse<String> response) {
		ApiClient.assertProblem(response, 401);
		assertThat(response.body()).doesNotContain("\"code\"");
	}

	@Test
	void theAccessTokenNamesItsSession() throws Exception {
		Session session = login();

		assertThat(sessionIdOf(session.accessToken())).hasSize(36);
		assertThat(me(session.accessToken()).statusCode()).isEqualTo(200);
	}

	@Test
	void aSecondLoginClosesTheFirstSessionAtOnce() throws Exception {
		Session pc = login();
		Session phone = login();

		assertReplaced(me(pc.accessToken()));
		assertReplaced(refresh(pc.refreshToken()));
		assertThat(me(phone.accessToken()).statusCode()).isEqualTo(200);
		assertThat(openSessions()).isEqualTo(1);
	}

	@Test
	void refreshingKeepsTheSameSession() throws Exception {
		Session first = login();

		Session refreshed = sessionOf(refresh(first.refreshToken()));

		assertThat(sessionIdOf(refreshed.accessToken())).isEqualTo(sessionIdOf(first.accessToken()));
		assertThat(me(first.accessToken()).statusCode()).isEqualTo(200);
		assertThat(me(refreshed.accessToken()).statusCode()).isEqualTo(200);
		assertThat(openSessions()).isEqualTo(1);
	}

	@Test
	void reusingARotatedRefreshTokenIsNotReportedAsAnotherDevice() throws Exception {
		Session first = login();
		sessionOf(refresh(first.refreshToken()));

		assertEndedWithoutReason(refresh(first.refreshToken()));
	}

	@Test
	void loggingOutClosesTheSessionAtOnce() throws Exception {
		Session session = login();

		assertThat(send("POST", "/api/v1/auth/logout", null, null, session.refreshToken()).statusCode()).isEqualTo(204);

		assertEndedWithoutReason(me(session.accessToken()));
		assertThat(openSessions()).isZero();
	}

	@Test
	void changingThePasswordKeepsTheCurrentSession() throws Exception {
		Session session = login();

		HttpResponse<String> changed = send("PUT", "/api/v1/auth/password",
				"{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Nueva12345\"}", session.accessToken(), null);

		assertThat(changed.statusCode()).as(changed.body()).isEqualTo(204);
		assertThat(me(session.accessToken()).statusCode()).isEqualTo(200);
		assertThat(refresh(session.refreshToken()).statusCode()).isEqualTo(200);
	}

	@Test
	void deactivatingTheAccountClosesItsSessionAtOnce() throws Exception {
		Session session = login();
		long userId = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		String admin = new ApiClient(port).login("admin@unmsm.edu.pe");

		HttpResponse<String> deactivated = send("PATCH", "/api/v1/admin/users/" + userId + "/active",
				"{\"active\":false}", admin, null);

		assertThat(deactivated.statusCode()).as(deactivated.body()).isEqualTo(200);
		assertEndedWithoutReason(me(session.accessToken()));
	}

	@Test
	void aTokenForASessionThatNeverExistedIsRejected() throws Exception {
		long userId = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		String forged = issuer.issue(new AuthenticatedUser(userId, email, "Pepe Sesión", "SOLICITANTE"),
				UUID.randomUUID().toString()).value();

		assertEndedWithoutReason(me(forged));
	}

	@Test
	void publicAuthRoutesIgnoreAClosedOrBrokenBearerToken() throws Exception {
		Session pc = login();
		Session phone = sessionOf(loginResponse(pc.accessToken()));

		assertThat(refresh(phone.refreshToken()).statusCode()).isEqualTo(200);
		HttpResponse<String> refreshedWithJunk = send("POST", "/api/v1/auth/refresh", null, "no.es.jwt",
				sessionOf(refresh(login().refreshToken())).refreshToken());
		assertThat(refreshedWithJunk.statusCode()).as(refreshedWithJunk.body()).isEqualTo(200);
		assertThat(send("POST", "/api/v1/auth/logout", null, "no.es.jwt", null).statusCode()).isEqualTo(204);
	}

	@Test
	void theChangePasswordRouteStillRequiresAValidToken() throws Exception {
		Session pc = login();
		login();

		assertReplaced(send("PUT", "/api/v1/auth/password",
				"{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Nueva12345\"}", pc.accessToken(), null));
	}

	@Test
	void simultaneousLoginsLeaveOneOpenSession() throws Exception {
		// Below the 5 attempts per email and address that the login limiter lets through at once.
		int logins = 4;
		ExecutorService pool = Executors.newFixedThreadPool(logins);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Integer>> results = new ArrayList<>();
			for (int i = 0; i < logins; i++) {
				Callable<Integer> attempt = () -> {
					start.await();
					return loginResponse(null).statusCode();
				};
				results.add(pool.submit(attempt));
			}
			start.countDown();
			for (Future<Integer> result : results) {
				assertThat(result.get()).isEqualTo(200);
			}
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(openSessions()).isEqualTo(1);
	}

	/**
	 * Found by the load test (infra/k6): the first logins of different users at the same moment deadlocked in MySQL
	 * (closing the other sessions locked index gaps those users shared) and some answered 500.
	 */
	@Test
	void simultaneousFirstLoginsOfDifferentUsersNeverFail() throws Exception {
		int users = 20;
		List<String> emails = new ArrayList<>();
		for (int i = 0; i < users; i++) {
			String other = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@unmsm.edu.pe";
			assertThat(send("POST", "/api/v1/auth/register", "{\"fullName\":\"Pepe Paralelo\",\"email\":\"" + other
					+ "\",\"password\":\"" + PASSWORD + "\",\"requesterType\":\"ESTUDIANTE\"}", null, null).statusCode())
					.isEqualTo(201);
			emails.add(other);
		}
		ExecutorService pool = Executors.newFixedThreadPool(users);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<HttpResponse<String>>> results = new ArrayList<>();
			for (String other : emails) {
				results.add(pool.submit(() -> {
					start.await();
					return send("POST", "/api/v1/auth/login",
							"{\"email\":\"" + other + "\",\"password\":\"" + PASSWORD + "\"}", null, null);
				}));
			}
			start.countDown();
			for (Future<HttpResponse<String>> result : results) {
				HttpResponse<String> response = result.get();
				assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
			}
		}
		finally {
			pool.shutdownNow();
		}
	}

	@Test
	void aPasskeyLoginAlsoClosesTheOtherSession() throws Exception {
		Session pc = login();
		TestPasskeyAuthenticator phone = new TestPasskeyAuthenticator("http://localhost:8088", "localhost");
		HttpResponse<String> options = send("POST", "/api/v1/me/passkeys/registration/options",
				"{\"currentPassword\":\"" + PASSWORD + "\"}", pc.accessToken(), null);
		assertThat(options.statusCode()).as(options.body()).isEqualTo(200);
		String credential = phone.register(ApiClient.read(options, "$.publicKey.challenge", String.class),
				ApiClient.read(options, "$.publicKey.user.id", String.class));
		HttpResponse<String> registered = send("POST", "/api/v1/me/passkeys", "{\"challengeId\":\""
				+ ApiClient.read(options, "$.challengeId", String.class) + "\",\"credential\":" + credential + "}",
				pc.accessToken(), null);
		assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);

		HttpResponse<String> challenge = send("POST", "/api/v1/auth/passkeys/login/options", null, null, null);
		HttpResponse<String> passkeyLogin = send("POST", "/api/v1/auth/passkeys/login", "{\"challengeId\":\""
				+ ApiClient.read(challenge, "$.challengeId", String.class) + "\",\"credential\":"
				+ phone.login(ApiClient.read(challenge, "$.publicKey.challenge", String.class)) + "}", null, null);

		Session viaPasskey = sessionOf(passkeyLogin);
		assertReplaced(me(pc.accessToken()));
		assertThat(me(viaPasskey.accessToken()).statusCode()).isEqualTo(200);
	}

}
