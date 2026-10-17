package pe.edu.unmsm.servicios.admin.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.assertProblem;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.read;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/** Changes shared catalogs (SLA hours, areas, locations), so its context and database are discarded afterwards. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class AdminCatalogsApiTests {

	private static final String ADMIN = "admin@unmsm.edu.pe";
	private static final String REQUESTER = "estudiante@unmsm.edu.pe";
	private static final String DESCRIPTION = "El proyector del aula no enciende desde ayer por la tarde";

	@Value("${local.server.port}")
	int port;

	private ApiClient api;
	private String admin;

	@BeforeEach
	void setUp() throws Exception {
		api = new ApiClient(port);
		admin = api.login(ADMIN);
	}

	private HttpResponse<String> catalogs() throws Exception {
		return api.get("/api/v1/catalogs", admin);
	}

	private long createArea(String name) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/admin/areas", "{\"name\":\"" + name + "\"}", admin);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return read(response, "$.id", Long.class);
	}

	@Test
	void onlyAdministratorsCanChangeCatalogs() throws Exception {
		String requester = api.login(REQUESTER);

		assertProblem(api.post("/api/v1/admin/areas", "{\"name\":\"Biblioteca\"}", requester), 403);
		assertProblem(api.send("PUT", "/api/v1/admin/priorities/1", "{\"slaHours\":5}",
				api.login("supervisor.mantenimiento@unmsm.edu.pe")), 403);
		assertProblem(api.send("DELETE", "/api/v1/admin/locations/3", null, api.login("tecnico.mantenimiento@unmsm.edu.pe")),
				403);
		assertThat(api.post("/api/v1/admin/areas", "{\"name\":\"Biblioteca\"}", null).statusCode()).isEqualTo(401);
	}

	@Test
	void anAreaIsCreatedDeactivatedAndReactivatedWithItsOriginalId() throws Exception {
		long id = createArea("Biblioteca Central");
		assertThat(read(catalogs(), "$.areas[*].name", List.class)).contains("Biblioteca Central");

		HttpResponse<String> duplicate = api.post("/api/v1/admin/areas", "{\"name\":\"biblioteca central\"}", admin);
		assertProblem(duplicate, 409);
		assertThat(read(duplicate, "$.errors.name", String.class)).isNotBlank();

		assertThat(api.send("DELETE", "/api/v1/admin/areas/" + id, null, admin).statusCode()).isEqualTo(204);
		assertThat(read(catalogs(), "$.areas[*].name", List.class)).doesNotContain("Biblioteca Central");
		assertProblem(api.send("DELETE", "/api/v1/admin/areas/" + id, null, admin), 404);

		assertThat(createArea("Biblioteca Central")).isEqualTo(id);
	}

	@Test
	void anAreaInUseCannotBeDeactivated() throws Exception {
		assertProblem(api.send("DELETE", "/api/v1/admin/areas/1", null, admin), 409);

		long area = createArea("Seguridad");
		HttpResponse<String> technician = api.post("/api/v1/admin/users",
				"{\"fullName\":\"Vigilante Uno\",\"email\":\"vigilante.uno@unmsm.edu.pe\",\"role\":\"TECNICO\",\"areaId\":"
						+ area + "}",
				admin);
		assertThat(technician.statusCode()).as(technician.body()).isEqualTo(201);

		assertProblem(api.send("DELETE", "/api/v1/admin/areas/" + area, null, admin), 409);
	}

	@Test
	void areaNamesAreValidated() throws Exception {
		HttpResponse<String> blank = api.post("/api/v1/admin/areas", "{\"name\":\"   \"}", admin);

		assertProblem(blank, 400);
		assertThat(read(blank, "$.errors.name", String.class)).isNotBlank();
	}

	@Test
	void aCategoryIsCreatedInAnActiveAreaAndCanBeDeactivated() throws Exception {
		HttpResponse<String> created = api.post("/api/v1/admin/categories", "{\"name\":\"Pintura\",\"areaId\":1}", admin);
		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		long id = read(created, "$.id", Long.class);
		assertThat(read(created, "$.areaId", Long.class)).isEqualTo(1L);
		assertThat(read(catalogs(), "$.categories[?(@.id == " + id + ")].areaId", List.class)).containsExactly(1);

		assertProblem(api.post("/api/v1/admin/categories", "{\"name\":\"PINTURA\",\"areaId\":1}", admin), 409);

		assertThat(api.send("DELETE", "/api/v1/admin/categories/" + id, null, admin).statusCode()).isEqualTo(204);
		assertThat(read(catalogs(), "$.categories[*].id", List.class)).doesNotContain((int) id);
		assertProblem(api.send("DELETE", "/api/v1/admin/categories/" + id, null, admin), 404);
	}

	@Test
	void aCategoryNeedsAnExistingActiveArea() throws Exception {
		long area = createArea("Temporal");
		api.send("DELETE", "/api/v1/admin/areas/" + area, null, admin);

		HttpResponse<String> inactive = api.post("/api/v1/admin/categories",
				"{\"name\":\"Huérfana\",\"areaId\":" + area + "}", admin);
		HttpResponse<String> unknown = api.post("/api/v1/admin/categories", "{\"name\":\"Huérfana\",\"areaId\":999}",
				admin);

		assertProblem(inactive, 400);
		assertThat(read(inactive, "$.errors.areaId", String.class)).isNotBlank();
		assertProblem(unknown, 400);
	}

	@Test
	void changingTheSlaHoursOnlyAffectsRequestsRegisteredAfterwards() throws Exception {
		String requester = api.login(REQUESTER);
		String body = "{\"categoryId\":1,\"locationId\":3,\"description\":\"" + DESCRIPTION + "\",\"priorityId\":3}";
		HttpResponse<String> before = api.post("/api/v1/requests", body, requester);
		assertThat(before.statusCode()).as(before.body()).isEqualTo(201);
		String code = read(before, "$.code", String.class);
		String dueBefore = read(before, "$.dueAt", String.class);

		HttpResponse<String> updated = api.send("PUT", "/api/v1/admin/priorities/3", "{\"slaHours\":10}", admin);

		assertThat(updated.statusCode()).as(updated.body()).isEqualTo(200);
		assertThat(read(updated, "$.slaHours", Integer.class)).isEqualTo(10);
		assertThat(read(api.get("/api/v1/requests/" + code, requester), "$.dueAt", String.class)).isEqualTo(dueBefore);
		HttpResponse<String> after = api.post("/api/v1/requests", body, requester);
		Duration sla = Duration.between(Instant.parse(read(after, "$.createdAt", String.class)),
				Instant.parse(read(after, "$.dueAt", String.class)));
		assertThat(sla).isEqualTo(Duration.ofHours(10));
	}

	@Test
	void slaHoursAreValidatedAndThePriorityMustExist() throws Exception {
		HttpResponse<String> zero = api.send("PUT", "/api/v1/admin/priorities/1", "{\"slaHours\":0}", admin);

		assertProblem(zero, 400);
		assertThat(read(zero, "$.errors.slaHours", String.class)).isNotBlank();
		assertProblem(api.send("PUT", "/api/v1/admin/priorities/99", "{\"slaHours\":5}", admin), 404);
	}

	@Test
	void aLocationCreatesItsMissingLevelsAndDeletingTheLastRoomPrunesThem() throws Exception {
		HttpResponse<String> created = api.post("/api/v1/admin/locations",
				"{\"faculty\":\"Facultad de Letras\",\"pavilion\":\"Pabellón B\",\"room\":\"Aula 305\"}", admin);
		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		long room = read(created, "$.id", Long.class);
		assertThat(read(created, "$.kind", String.class)).isEqualTo("AMBIENTE");
		assertThat(read(catalogs(), "$.locations[*].name", List.class)).contains("Facultad de Letras", "Pabellón B",
				"Aula 305");

		HttpResponse<String> duplicate = api.post("/api/v1/admin/locations",
				"{\"faculty\":\"Facultad de Letras\",\"pavilion\":\"Pabellón B\",\"room\":\"aula 305\"}", admin);
		assertProblem(duplicate, 409);
		assertThat(read(duplicate, "$.errors.room", String.class)).isNotBlank();

		assertThat(api.send("DELETE", "/api/v1/admin/locations/" + room, null, admin).statusCode()).isEqualTo(204);
		assertThat(read(catalogs(), "$.locations[*].name", List.class)).doesNotContain("Facultad de Letras",
				"Pabellón B", "Aula 305");
	}

	@Test
	void anExistingFacultyAndPavilionAreReused() throws Exception {
		HttpResponse<String> created = api.post("/api/v1/admin/locations",
				"{\"faculty\":\"Facultad de Ingeniería de Sistemas e Informática\",\"pavilion\":\"Pabellón Principal\","
						+ "\"room\":\"Laboratorio 9\"}",
				admin);

		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		assertThat(read(created, "$.parentId", Long.class)).isEqualTo(2L);
	}

	@Test
	void onlyRoomsWithoutRequestsCanBeDeleted() throws Exception {
		assertProblem(api.send("DELETE", "/api/v1/admin/locations/2", null, admin), 409);
		String body = "{\"categoryId\":1,\"locationId\":4,\"description\":\"" + DESCRIPTION + "\",\"priorityId\":2}";
		assertThat(api.post("/api/v1/requests", body, api.login(REQUESTER)).statusCode()).isEqualTo(201);

		assertProblem(api.send("DELETE", "/api/v1/admin/locations/4", null, admin), 409);
		assertProblem(api.send("DELETE", "/api/v1/admin/locations/9999", null, admin), 404);
	}

	@Test
	void locationFieldsAreValidated() throws Exception {
		HttpResponse<String> response = api.post("/api/v1/admin/locations",
				"{\"faculty\":\"FISI\",\"pavilion\":\"\",\"room\":\"Aula 1\"}", admin);

		assertProblem(response, 400);
		assertThat(read(response, "$.errors.pavilion", String.class)).isNotBlank();
	}

}
