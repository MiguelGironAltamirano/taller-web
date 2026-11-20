package pe.edu.unmsm.servicios.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ApiConventionsTests {

	@Value("${local.server.port}")
	int port;

	/** Tokens here belong to made-up users with no real session: every session counts as open. */
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	pe.edu.unmsm.servicios.shared.application.SessionRegistry sessions;

	@org.junit.jupiter.api.BeforeEach
	void everySessionIsOpen() {
		org.mockito.Mockito.when(sessions.stateOf(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(pe.edu.unmsm.servicios.shared.application.SessionRegistry.SessionState.OPEN);
	}

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void systemInfoReturnsNameAndVersion() throws Exception {
		HttpResponse<String> response = get("/api/v1/system/info");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"name\":\"servicios\"").contains("\"version\":\"0.0.1-SNAPSHOT\"");
	}

	@org.springframework.beans.factory.annotation.Autowired
	pe.edu.unmsm.servicios.shared.infrastructure.security.AccessTokenIssuer issuer;

	@Test
	void unknownApiRouteWithoutTokenReturnsUnauthorizedProblem() throws Exception {
		HttpResponse<String> response = get("/api/v1/does-not-exist");
		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

	@Test
	void unknownApiRouteWithTokenReturnsNotFoundProblem() throws Exception {
		String token = issuer.issue(new pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser(
				1, "admin@unmsm.edu.pe", "Admin", "ADMINISTRADOR"), "test-session").value();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/does-not-exist"))
				.header("Authorization", "Bearer " + token).GET().build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

	@Test
	void openApiDocumentIsPublished() throws Exception {
		HttpResponse<String> response = get("/v3/api-docs");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("/api/v1/system/info");
	}

}
