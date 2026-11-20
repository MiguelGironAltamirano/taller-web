package pe.edu.unmsm.servicios.auth.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import pe.edu.unmsm.servicios.testsupport.TestPasskeyAuthenticator;

/** Login with fingerprint or passkey, docs/api/passkeys-v1.md, with a software authenticator. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class PasskeyApiTests {

	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String TEACHER = "docente@unmsm.edu.pe";
	private static final String ORIGIN = "http://localhost:8088";
	private static final String RP_ID = "localhost";
	/** Each test logs in from its own address, so failed attempts of one test never block another. */
	private static final AtomicInteger NEXT_CLIENT = new AtomicInteger(1);

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();
	private ApiClient api;
	private String clientIp;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		clientIp = "10.77.0." + NEXT_CLIENT.getAndIncrement();
		jdbc.update("delete from passkeys");
		jdbc.update("delete from webauthn_challenges");
		jdbc.update("update users set webauthn_user_handle = null");
		jdbc.update("update users set active = true where email in (?, ?)", STUDENT, TEACHER);
	}

	private HttpResponse<String> post(String path, String json, String token) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.header("X-Forwarded-For", clientIp)
				.POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json));
		if (token != null) {
			request.header("Authorization", "Bearer " + token);
		}
		return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> registrationOptions(String token, String password) throws Exception {
		return post("/api/v1/me/passkeys/registration/options", "{\"currentPassword\":\"" + password + "\"}", token);
	}

	/** Registers the authenticator for the user of {@code token} and returns the created passkey response. */
	private HttpResponse<String> register(String token, TestPasskeyAuthenticator authenticator, String name)
			throws Exception {
		HttpResponse<String> options = registrationOptions(token, ApiClient.DEMO_PASSWORD);
		assertThat(options.statusCode()).as(options.body()).isEqualTo(200);
		String credential = authenticator.register(ApiClient.read(options, "$.publicKey.challenge", String.class),
				ApiClient.read(options, "$.publicKey.user.id", String.class));
		String nameField = name == null ? "" : ",\"name\":\"" + name + "\"";
		return post("/api/v1/me/passkeys", "{\"challengeId\":\"" + ApiClient.read(options, "$.challengeId", String.class)
				+ "\"" + nameField + ",\"credential\":" + credential + "}", token);
	}

	private record LoginChallenge(String id, String challenge) {
	}

	private LoginChallenge loginChallenge() throws Exception {
		HttpResponse<String> options = post("/api/v1/auth/passkeys/login/options", null, null);
		assertThat(options.statusCode()).as(options.body()).isEqualTo(200);
		return new LoginChallenge(ApiClient.read(options, "$.challengeId", String.class),
				ApiClient.read(options, "$.publicKey.challenge", String.class));
	}

	private HttpResponse<String> login(String challengeId, String credential) throws Exception {
		return post("/api/v1/auth/passkeys/login",
				"{\"challengeId\":\"" + challengeId + "\",\"credential\":" + credential + "}", null);
	}

	private HttpResponse<String> login(TestPasskeyAuthenticator authenticator) throws Exception {
		LoginChallenge challenge = loginChallenge();
		return login(challenge.id(), authenticator.login(challenge.challenge()));
	}

	// ───── registration ─────

	@Test
	void theRegistrationOptionsFollowTheContract() throws Exception {
		HttpResponse<String> options = registrationOptions(api.login(STUDENT), ApiClient.DEMO_PASSWORD);

		assertThat(options.statusCode()).as(options.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(options, "$.challengeId")).hasSize(36);
		assertThat(ApiClient.<String>read(options, "$.publicKey.challenge")).hasSize(43);
		assertThat(ApiClient.<String>read(options, "$.publicKey.rp.id")).isEqualTo(RP_ID);
		assertThat(ApiClient.<String>read(options, "$.publicKey.rp.name")).isEqualTo("Servicios Universitarios UNMSM");
		assertThat(ApiClient.<String>read(options, "$.publicKey.user.name")).isEqualTo(STUDENT);
		assertThat(ApiClient.<String>read(options, "$.publicKey.user.displayName")).isEqualTo("Estudiante Demo");
		assertThat(ApiClient.<String>read(options, "$.publicKey.user.id")).as("64 random bytes").hasSize(86);
		assertThat(ApiClient.<List<Integer>>read(options, "$.publicKey.pubKeyCredParams[*].alg"))
				.containsExactly(-7, -257);
		assertThat(ApiClient.<String>read(options, "$.publicKey.authenticatorSelection.residentKey"))
				.isEqualTo("required");
		assertThat(ApiClient.<String>read(options, "$.publicKey.authenticatorSelection.userVerification"))
				.isEqualTo("required");
		assertThat(ApiClient.<String>read(options, "$.publicKey.attestation")).isEqualTo("none");
		assertThat(ApiClient.<List<Object>>read(options, "$.publicKey.excludeCredentials")).isEmpty();
	}

	@Test
	void registeringAPasskeyRequiresTheCurrentPasswordAndAToken() throws Exception {
		String token = api.login(STUDENT);

		HttpResponse<String> wrong = registrationOptions(token, "Incorrecta123");
		ApiClient.assertProblem(wrong, 400);
		assertThat(ApiClient.<String>read(wrong, "$.errors.currentPassword")).isNotBlank();
		ApiClient.assertProblem(post("/api/v1/me/passkeys/registration/options", "{}", token), 400);
		ApiClient.assertProblem(registrationOptions(null, ApiClient.DEMO_PASSWORD), 401);
		ApiClient.assertProblem(post("/api/v1/me/passkeys", "{}", null), 401);
		assertThat(api.get("/api/v1/me/passkeys", null).statusCode()).isEqualTo(401);
	}

	@Test
	void aRegisteredPasskeyIsListedWithItsName() throws Exception {
		String token = api.login(STUDENT);

		HttpResponse<String> created = register(token, new TestPasskeyAuthenticator(ORIGIN, RP_ID), "Mi laptop");

		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		assertThat(ApiClient.<String>read(created, "$.name")).isEqualTo("Mi laptop");
		assertThat(ApiClient.<Boolean>read(created, "$.synced")).isFalse();
		assertThat(ApiClient.<Object>read(created, "$.lastUsedAt")).isNull();
		HttpResponse<String> list = api.get("/api/v1/me/passkeys", token);
		assertThat(ApiClient.<List<String>>read(list, "$[*].name")).containsExactly("Mi laptop");
		assertThat(ApiClient.<List<Object>>read(api.get("/api/v1/me/passkeys", api.login(TEACHER)), "$"))
				.as("each user only sees their own").isEmpty();
	}

	@Test
	void withoutNameThePasskeyIsNamedAfterItsDate() throws Exception {
		HttpResponse<String> created = register(api.login(STUDENT), new TestPasskeyAuthenticator(ORIGIN, RP_ID), null);

		assertThat(ApiClient.<String>read(created, "$.name")).matches("Passkey del \\d{2}/\\d{2}/\\d{4}");
	}

	@Test
	void theSameAuthenticatorCannotBeRegisteredTwice() throws Exception {
		String token = api.login(STUDENT);
		TestPasskeyAuthenticator authenticator = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		register(token, authenticator, "Primera");

		HttpResponse<String> options = registrationOptions(token, ApiClient.DEMO_PASSWORD);
		assertThat(ApiClient.<List<String>>read(options, "$.publicKey.excludeCredentials[*].id"))
				.containsExactly(authenticator.credentialId());
		ApiClient.assertProblem(register(token, authenticator, "Otra vez"), 409);
	}

	@Test
	void aRegistrationIsRejectedWithAForeignOriginOrAnotherUsersChallenge() throws Exception {
		String student = api.login(STUDENT);
		ApiClient.assertProblem(register(student, new TestPasskeyAuthenticator("https://evil.example", RP_ID), null),
				400);
		ApiClient.assertProblem(register(student, new TestPasskeyAuthenticator(ORIGIN, "evil.example"), null), 400);

		HttpResponse<String> options = registrationOptions(student, ApiClient.DEMO_PASSWORD);
		String credential = new TestPasskeyAuthenticator(ORIGIN, RP_ID).register(
				ApiClient.read(options, "$.publicKey.challenge", String.class),
				ApiClient.read(options, "$.publicKey.user.id", String.class));
		HttpResponse<String> stolen = post("/api/v1/me/passkeys", "{\"challengeId\":\""
				+ ApiClient.read(options, "$.challengeId", String.class) + "\",\"credential\":" + credential + "}",
				api.login(TEACHER));
		ApiClient.assertProblem(stolen, 400);
		assertThat(jdbc.queryForObject("select count(*) from passkeys", Integer.class)).isZero();
	}

	@Test
	void aUserHasAtMostTenPasskeys() throws Exception {
		String token = api.login(STUDENT);
		for (int i = 0; i < 10; i++) {
			assertThat(register(token, new TestPasskeyAuthenticator(ORIGIN, RP_ID), "Equipo " + i).statusCode())
					.isEqualTo(201);
		}

		ApiClient.assertProblem(registrationOptions(token, ApiClient.DEMO_PASSWORD), 409);
	}

	// ───── login ─────

	@Test
	void theLoginOptionsAskForAVerifiedUserWithoutRevealingAnyAccount() throws Exception {
		HttpResponse<String> options = post("/api/v1/auth/passkeys/login/options", null, null);

		assertThat(options.statusCode()).as(options.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(options, "$.publicKey.rpId")).isEqualTo(RP_ID);
		assertThat(ApiClient.<String>read(options, "$.publicKey.userVerification")).isEqualTo("required");
		assertThat(ApiClient.<Integer>read(options, "$.publicKey.timeout")).isEqualTo(60000);
		assertThat(ApiClient.<List<Object>>read(options, "$.publicKey.allowCredentials")).isEmpty();
	}

	@Test
	void aPasskeyOpensTheSameSessionAsThePassword() throws Exception {
		TestPasskeyAuthenticator authenticator = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		register(api.login(STUDENT), authenticator, "Mi laptop");

		HttpResponse<String> response = login(authenticator);

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(ApiClient.<String>read(response, "$.user.email")).isEqualTo(STUDENT);
		assertThat(response.headers().firstValue("Set-Cookie")).hasValueSatisfying(cookie -> assertThat(cookie)
				.startsWith("refresh_token=").contains("HttpOnly").contains("SameSite=Strict"));
		String accessToken = ApiClient.read(response, "$.accessToken", String.class);
		assertThat(ApiClient.<String>read(api.get("/api/v1/me", accessToken), "$.email")).isEqualTo(STUDENT);
		assertThat(ApiClient.<String>read(api.get("/api/v1/me/passkeys", accessToken), "$[0].lastUsedAt"))
				.isNotBlank();
		assertThat(login(authenticator).statusCode()).as("the counter grows on every login").isEqualTo(200);
	}

	@Test
	void aLoginChallengeServesOnlyOnce() throws Exception {
		TestPasskeyAuthenticator authenticator = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		register(api.login(STUDENT), authenticator, null);
		LoginChallenge challenge = loginChallenge();

		assertThat(login(challenge.id(), authenticator.login(challenge.challenge())).statusCode()).isEqualTo(200);
		ApiClient.assertProblem(login(challenge.id(), authenticator.login(challenge.challenge())), 401);
		ApiClient.assertProblem(login("no-existe", authenticator.login(challenge.challenge())), 401);
	}

	@Test
	void everyFailedCheckIsTheSameGenericUnauthorized() throws Exception {
		TestPasskeyAuthenticator registered = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		register(api.login(STUDENT), registered, null);
		LoginChallenge challenge;

		HttpResponse<String> unknown = login(new TestPasskeyAuthenticator(ORIGIN, RP_ID).registeredLess());
		ApiClient.assertProblem(unknown, 401);
		assertThat(ApiClient.<String>read(unknown, "$.detail")).isEqualTo("No pudimos verificar tu passkey");

		challenge = loginChallenge();
		ApiClient.assertProblem(login(challenge.id(), registered.login(challenge.challenge(), false, 50)), 401);

		challenge = loginChallenge();
		ApiClient.assertProblem(login(challenge.id(), registered.login("b3RybyBkZXNhZsOtbw", true, 51)), 401);

		challenge = loginChallenge();
		ApiClient.assertProblem(login(challenge.id(), "{\"id\":\"x\"}"), 401);
	}

	@Test
	void aPasskeyLoginFromAnotherOriginIsRejected() throws Exception {
		TestPasskeyAuthenticator authenticator = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		register(api.login(STUDENT), authenticator, null);
		TestPasskeyAuthenticator phishing = authenticator.from("https://evil.example");

		ApiClient.assertProblem(login(phishing), 401);
	}

	@Test
	void aClonedAuthenticatorWhoseCounterDoesNotGrowIsRejected() throws Exception {
		TestPasskeyAuthenticator authenticator = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		register(api.login(STUDENT), authenticator, null);
		LoginChallenge first = loginChallenge();
		assertThat(login(first.id(), authenticator.login(first.challenge(), true, 5)).statusCode()).isEqualTo(200);

		LoginChallenge second = loginChallenge();
		ApiClient.assertProblem(login(second.id(), authenticator.login(second.challenge(), true, 3)), 401);
	}

	@Test
	void aDeactivatedUserCannotLogInWithTheirPasskey() throws Exception {
		TestPasskeyAuthenticator authenticator = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		register(api.login(STUDENT), authenticator, null);
		jdbc.update("update users set active = false where email = ?", STUDENT);

		ApiClient.assertProblem(login(authenticator), 401);
	}

	@Test
	void tooManyFailedPasskeyLoginsFromOneClientAreLimited() throws Exception {
		TestPasskeyAuthenticator unknown = new TestPasskeyAuthenticator(ORIGIN, RP_ID).registeredLess();
		for (int i = 0; i < 5; i++) {
			ApiClient.assertProblem(login(unknown), 401);
		}

		HttpResponse<String> limited = login(unknown);

		ApiClient.assertProblem(limited, 429);
		assertThat(limited.headers().firstValue("Retry-After")).isPresent();
		clientIp = "10.77.1." + NEXT_CLIENT.getAndIncrement();
		ApiClient.assertProblem(login(unknown), 401);
	}

	// ───── rename and delete ─────

	@Test
	void aPasskeyCanBeRenamedAndDeletedOnlyByItsOwner() throws Exception {
		String token = api.login(STUDENT);
		TestPasskeyAuthenticator authenticator = new TestPasskeyAuthenticator(ORIGIN, RP_ID);
		long id = ApiClient.read(register(token, authenticator, "Vieja"), "$.id", Long.class);
		String path = "/api/v1/me/passkeys/" + id;
		String teacher = api.login(TEACHER);

		ApiClient.assertProblem(api.send("PATCH", path, "{\"name\":\"Ajena\"}", teacher), 404);
		ApiClient.assertProblem(api.send("PATCH", path, "{\"name\":\"  \"}", token), 400);
		ApiClient.assertProblem(api.send("PATCH", path, "{\"name\":\"" + "x".repeat(61) + "\"}", token), 400);
		HttpResponse<String> renamed = api.send("PATCH", path, "{\"name\":\"  Celular  \"}", token);
		assertThat(ApiClient.<String>read(renamed, "$.name")).isEqualTo("Celular");

		ApiClient.assertProblem(api.send("DELETE", path, null, teacher), 404);
		assertThat(api.send("DELETE", path, null, token).statusCode()).isEqualTo(204);
		ApiClient.assertProblem(api.send("DELETE", path, null, token), 404);
		ApiClient.assertProblem(login(authenticator), 401);
	}

}
