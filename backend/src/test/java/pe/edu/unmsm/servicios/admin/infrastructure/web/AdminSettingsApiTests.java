package pe.edu.unmsm.servicios.admin.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.assertProblem;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.read;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.shared.application.SystemSettingsProvider;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/** Changes the global system settings, so its context and database are discarded afterwards. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class AdminSettingsApiTests {

	private static final String ADMIN = "admin@unmsm.edu.pe";
	private static final String CONFIG = "/api/v1/admin/config";

	@Value("${local.server.port}")
	int port;

	@Autowired
	SystemSettingsProvider settingsProvider;

	private ApiClient api;
	private String admin;

	@BeforeEach
	void setUp() throws Exception {
		api = new ApiClient(port);
		admin = api.login(ADMIN);
	}

	@Test
	void onlyAdministratorsCanReadOrChangeTheConfiguration() throws Exception {
		String supervisor = api.login("supervisor.mantenimiento@unmsm.edu.pe");
		String body = "{\"autoCloseHours\":24,\"emailNotifications\":true,\"triageSuggestions\":true}";

		assertProblem(api.get(CONFIG, supervisor), 403);
		assertProblem(api.send("PUT", CONFIG, body, api.login("estudiante@unmsm.edu.pe")), 403);
		assertThat(api.get(CONFIG, null).statusCode()).isEqualTo(401);
	}

	@Test
	void theConfigurationIsUpdatedAndVisibleToOtherModulesThroughTheSettingsPort() throws Exception {
		HttpResponse<String> updated = api.send("PUT", CONFIG,
				"{\"autoCloseHours\":48,\"emailNotifications\":true,\"triageSuggestions\":false}", admin);

		assertThat(updated.statusCode()).as(updated.body()).isEqualTo(200);
		assertThat(read(updated, "$.autoCloseHours", Integer.class)).isEqualTo(48);
		HttpResponse<String> current = api.get(CONFIG, admin);
		assertThat(read(current, "$.autoCloseHours", Integer.class)).isEqualTo(48);
		assertThat(read(current, "$.emailNotifications", Boolean.class)).isTrue();
		assertThat(read(current, "$.triageSuggestions", Boolean.class)).isFalse();
		assertThat(settingsProvider.current()).isEqualTo(new SystemSettings(48, true, false));
	}

	@Test
	void invalidConfigurationIsRejectedWithFieldErrors() throws Exception {
		HttpResponse<String> zero = api.send("PUT", CONFIG,
				"{\"autoCloseHours\":0,\"emailNotifications\":true,\"triageSuggestions\":true}", admin);
		assertProblem(zero, 400);
		assertThat(read(zero, "$.errors.autoCloseHours", String.class)).isNotBlank();

		HttpResponse<String> missing = api.send("PUT", CONFIG, "{\"autoCloseHours\":24}", admin);
		assertProblem(missing, 400);
		assertThat(read(missing, "$.errors.emailNotifications", String.class)).isNotBlank();
		assertThat(read(missing, "$.errors.triageSuggestions", String.class)).isNotBlank();
	}

}
