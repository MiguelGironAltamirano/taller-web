package pe.edu.unmsm.servicios.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SecurityFoundationTests {

	@Value("${local.server.port}")
	int port;

	@Autowired
	AccessTokenIssuer issuer;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> get(String path, String token) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void meWithoutTokenReturnsUnauthorizedProblem() throws Exception {
		HttpResponse<String> response = get("/api/v1/me", null);
		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

	@Test
	void meWithValidTokenReturnsTheAuthenticatedUser() throws Exception {
		String token = issuer.issue(new AuthenticatedUser(42, "ana@unmsm.edu.pe", "Ana Pérez", "SOLICITANTE")).value();
		HttpResponse<String> response = get("/api/v1/me", token);
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"id\":42").contains("\"email\":\"ana@unmsm.edu.pe\"")
				.contains("\"role\":\"SOLICITANTE\"");
	}

	@Test
	void meReturnsTheAreaClaimWhenPresentAndNullOtherwise() throws Exception {
		String supervisor = issuer.issue(new AuthenticatedUser(7, "s@unmsm.edu.pe", "Sup", "SUPERVISOR", 3L)).value();
		String requester = issuer.issue(new AuthenticatedUser(8, "r@unmsm.edu.pe", "Req", "SOLICITANTE", null)).value();
		assertThat(get("/api/v1/me", supervisor).body()).contains("\"areaId\":3");
		assertThat(get("/api/v1/me", requester).body()).contains("\"areaId\":null");
	}

	@Test
	void meWithTamperedTokenReturnsUnauthorized() throws Exception {
		String token = issuer.issue(new AuthenticatedUser(42, "ana@unmsm.edu.pe", "Ana Pérez", "SOLICITANTE")).value();
		String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
		assertThat(get("/api/v1/me", tampered).statusCode()).isEqualTo(401);
	}

	@Test
	void accessTokenLastsFifteenMinutes() {
		long expiresIn = issuer.issue(new AuthenticatedUser(1, "a@unmsm.edu.pe", "A", "ADMINISTRADOR")).expiresInSeconds();
		assertThat(expiresIn).isEqualTo(900);
	}

}
