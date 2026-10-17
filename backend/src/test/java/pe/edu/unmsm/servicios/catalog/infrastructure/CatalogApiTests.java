package pe.edu.unmsm.servicios.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class CatalogApiTests {

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
	}

	@AfterEach
	void restoreCatalogs() {
		jdbc.update("update categories set active = true");
		jdbc.update("update areas set active = true");
	}

	@Test
	void catalogsRequireAuthentication() throws Exception {
		ApiClient.assertProblem(api.get("/api/v1/catalogs", null), 401);
	}

	@Test
	void returnsTheSeededAreasCategoriesPrioritiesAndLocations() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/catalogs", api.login("estudiante@unmsm.edu.pe"));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(ApiClient.<List<Object>>read(response, "$.areas")).hasSize(4);
		assertThat(ApiClient.<List<Object>>read(response, "$.categories")).hasSize(7);
		assertThat(ApiClient.<List<Object>>read(response, "$.priorities")).hasSize(4);
		assertThat(ApiClient.<List<Object>>read(response, "$.locations")).hasSize(8);
	}

	@Test
	void prioritiesComeOrderedWithTheirSlaHours() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/catalogs", api.login("estudiante@unmsm.edu.pe"));

		assertThat(ApiClient.<List<String>>read(response, "$.priorities[*].name"))
				.containsExactly("Crítica", "Alta", "Media", "Baja");
		assertThat(ApiClient.<List<Integer>>read(response, "$.priorities[*].slaHours"))
				.containsExactly(8, 24, 72, 120);
	}

	@Test
	void categoriesPointToTheirDefaultArea() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/catalogs", api.login("estudiante@unmsm.edu.pe"));

		assertThat(ApiClient.<List<Integer>>read(response, "$.categories[?(@.name=='Red e Internet')].areaId"))
				.containsExactly(3);
		assertThat(ApiClient.<List<Integer>>read(response, "$.categories[?(@.name=='Electricidad')].areaId"))
				.containsExactly(1);
	}

	@Test
	void locationsExposeKindAndParentSoTheTreeCanBeBuilt() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/catalogs", api.login("estudiante@unmsm.edu.pe"));

		assertThat(ApiClient.<List<String>>read(response, "$.locations[*].kind").stream().distinct())
				.containsExactlyInAnyOrder("FACULTAD", "PABELLON", "AMBIENTE");
		assertThat(ApiClient.<List<Integer>>read(response, "$.locations[?(@.name=='Aula 101')].parentId"))
				.containsExactly(2);
		// JsonPath drops null leaves in filters, so the whole objects are checked: the field is present and null.
		assertThat(ApiClient.<List<Map<String, Object>>>read(response, "$.locations[?(@.kind=='FACULTAD')]"))
				.hasSize(2).allSatisfy(location -> assertThat(location).containsEntry("parentId", null));
	}

	@Test
	void inactiveCategoriesAndAreasAreNotListed() throws Exception {
		jdbc.update("update categories set active = false where id = 7");
		jdbc.update("update areas set active = false where id = 4");

		HttpResponse<String> response = api.get("/api/v1/catalogs", api.login("estudiante@unmsm.edu.pe"));

		assertThat(ApiClient.<List<String>>read(response, "$.categories[*].name"))
				.hasSize(6).doesNotContain("Trámites administrativos");
		assertThat(ApiClient.<List<String>>read(response, "$.areas[*].name"))
				.hasSize(3).doesNotContain("Servicios Administrativos");
	}

}
