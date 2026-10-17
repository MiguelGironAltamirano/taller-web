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

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CorsDisabledByDefaultTests {

	@Value("${local.server.port}")
	int port;

	@Test
	void noCrossOriginIsAllowedWhenNoOriginsAreConfigured() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/me"))
				.header("Origin", "https://app.example.pe")
				.header("Access-Control-Request-Method", "GET")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
	}

}
