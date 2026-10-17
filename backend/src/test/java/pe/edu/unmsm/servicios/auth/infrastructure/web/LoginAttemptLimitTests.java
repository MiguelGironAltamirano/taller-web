package pe.edu.unmsm.servicios.auth.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import({ TestcontainersConfiguration.class, LoginAttemptLimitTests.MutableClockConfig.class })
class LoginAttemptLimitTests {

	static final class MutableClock extends Clock {

		private volatile Instant now = Instant.now();

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class MutableClockConfig {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock();
		}

	}

	@Value("${local.server.port}")
	int port;

	@Autowired
	MutableClock clock;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> login(String email, String password) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
				.header("Content-Type", "application/json")
				.POST(BodyPublishers.ofString("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static String uniqueEmail() {
		return "l" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@unmsm.edu.pe";
	}

	private void failFiveTimes(String email) throws Exception {
		for (int i = 0; i < 5; i++) {
			assertThat(login(email, "Incorrecta1").statusCode()).isEqualTo(401);
		}
	}

	@Test
	void sixthFailedAttemptIsRateLimitedWithRetryAfter() throws Exception {
		String email = uniqueEmail();
		failFiveTimes(email);

		HttpResponse<String> response = login(email, "Incorrecta1");

		assertThat(response.statusCode()).isEqualTo(429);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
		assertThat(response.headers().firstValue("Retry-After")).hasValueSatisfying(
				seconds -> assertThat(Long.parseLong(seconds)).isBetween(1L, 900L));
	}

	@Test
	void blockIsKeyedByLowercasedEmailAndDoesNotAffectOtherAccounts() throws Exception {
		String email = uniqueEmail();
		failFiveTimes(email);

		assertThat(login(email.toUpperCase(), "Incorrecta1").statusCode()).isEqualTo(429);
		assertThat(login("estudiante@unmsm.edu.pe", "Demo12345!").statusCode()).isEqualTo(200);
	}

	@Test
	void attemptsAreAllowedAgainAfterTheWindow() throws Exception {
		String email = uniqueEmail();
		failFiveTimes(email);
		assertThat(login(email, "Incorrecta1").statusCode()).isEqualTo(429);

		clock.advance(Duration.ofMinutes(15).plusSeconds(1));

		assertThat(login(email, "Incorrecta1").statusCode()).isEqualTo(401);
	}

	@Test
	void successfulLoginResetsTheCounter() throws Exception {
		String email = "supervisor.mantenimiento@unmsm.edu.pe";
		for (int i = 0; i < 4; i++) {
			assertThat(login(email, "Incorrecta1").statusCode()).isEqualTo(401);
		}
		assertThat(login(email, "Demo12345!").statusCode()).isEqualTo(200);

		for (int i = 0; i < 4; i++) {
			assertThat(login(email, "Incorrecta1").statusCode()).isEqualTo(401);
		}
		assertThat(login(email, "Demo12345!").statusCode()).isEqualTo(200);
	}

}
