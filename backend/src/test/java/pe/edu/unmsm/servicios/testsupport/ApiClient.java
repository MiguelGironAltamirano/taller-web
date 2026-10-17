package pe.edu.unmsm.servicios.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.jayway.jsonpath.JsonPath;

/** Minimal HTTP client for the API integration tests: real server, real MySQL, real JWTs. */
public final class ApiClient {

	public static final String DEMO_PASSWORD = "Demo12345!";

	private final HttpClient http = HttpClient.newHttpClient();
	private final int port;

	public ApiClient(int port) {
		this.port = port;
	}

	/** Logs in with the demo password and returns the access token. */
	public String login(String email) throws Exception {
		HttpResponse<String> response = post("/api/v1/auth/login",
				"{\"email\":\"" + email + "\",\"password\":\"" + DEMO_PASSWORD + "\"}", null);
		assertThat(response.statusCode()).as("login " + email + ": " + response.body()).isEqualTo(200);
		return read(response, "$.accessToken", String.class);
	}

	public HttpResponse<String> get(String path, String token) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	public HttpResponse<String> post(String path, String json, String token) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json));
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	public static <T> T read(HttpResponse<String> response, String jsonPath, Class<T> type) {
		return JsonPath.parse(response.body()).read(jsonPath, type);
	}

	public static <T> T read(HttpResponse<String> response, String jsonPath) {
		return JsonPath.parse(response.body()).read(jsonPath);
	}

	public static void assertProblem(HttpResponse<String> response, int status) {
		assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

}
