package pe.edu.unmsm.servicios.shared.infrastructure.security;

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

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "app.cors.allowed-origins=https://app.example.pe, https://staging.example.pe")
@Import(TestcontainersConfiguration.class)
class SecurityHeadersAndCorsTests {

	@Value("${local.server.port}")
	int port;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> preflight(String origin) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/me"))
				.header("Origin", origin)
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "authorization")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void preflightFromAllowedOriginIsAccepted() throws Exception {
		HttpResponse<String> response = preflight("https://staging.example.pe");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).hasValue("https://staging.example.pe");
		assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");
		assertThat(response.headers().firstValue("Access-Control-Allow-Methods")).hasValueSatisfying(
				methods -> assertThat(methods).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
	}

	@Test
	void preflightFromAnotherOriginIsRejected() throws Exception {
		HttpResponse<String> response = preflight("https://evil.example.com");

		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(response.statusCode()).isEqualTo(403);
	}

	@Test
	void responsesCarrySecurityHeaders() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/system/info"))
				.GET().build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
		assertThat(response.headers().firstValue("X-Frame-Options")).hasValue("DENY");
		assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
	}

}
