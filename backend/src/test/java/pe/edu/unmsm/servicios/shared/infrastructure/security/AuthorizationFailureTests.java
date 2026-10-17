package pe.edu.unmsm.servicios.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({ TestcontainersConfiguration.class, AuthorizationFailureTests.AdminOnlyEndpoint.class })
class AuthorizationFailureTests {

	/** Test-only endpoint: there is intentionally no production endpoint for this check. */
	@TestConfiguration(proxyBeanMethods = false)
	@RestController
	static class AdminOnlyEndpoint {

		@GetMapping("/api/v1/test/admin-only")
		@PreAuthorize("hasRole('ADMINISTRADOR')")
		String adminOnly() {
			return "ok";
		}

	}

	@Value("${local.server.port}")
	int port;

	@Autowired
	AccessTokenIssuer issuer;

	@Autowired
	JwtEncoder encoder;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> get(String path, String token) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Authorization", "Bearer " + token).GET().build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static void assertProblem(HttpResponse<String> response, int status) {
		assertThat(response.statusCode()).isEqualTo(status);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

	@Test
	void expiredAccessTokenReturnsUnauthorizedProblem() throws Exception {
		Instant issuedAt = Instant.now().minusSeconds(3600);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("servicios-unmsm")
				.subject("42")
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(900))
				.claim("email", "ana@unmsm.edu.pe")
				.claim("name", "Ana Pérez")
				.claim("roles", List.of("SOLICITANTE"))
				.build();
		String expired = encoder.encode(
				JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		assertProblem(get("/api/v1/me", expired), 401);
	}

	@Test
	void roleWithoutPermissionReturnsForbiddenProblem() throws Exception {
		String requester = issuer.issue(new AuthenticatedUser(42, "ana@unmsm.edu.pe", "Ana Pérez", "SOLICITANTE")).value();

		assertProblem(get("/api/v1/test/admin-only", requester), 403);
	}

	@Test
	void roleWithPermissionIsAllowed() throws Exception {
		String admin = issuer.issue(new AuthenticatedUser(1, "admin@unmsm.edu.pe", "Admin", "ADMINISTRADOR")).value();

		HttpResponse<String> response = get("/api/v1/test/admin-only", admin);

		assertThat(response.statusCode()).isEqualTo(200);
	}

}
