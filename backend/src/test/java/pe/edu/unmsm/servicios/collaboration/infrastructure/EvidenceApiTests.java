package pe.edu.unmsm.servicios.collaboration.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class EvidenceApiTests {

	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String OTHER_STUDENT = "otra.solicitante@unmsm.edu.pe";
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	private static final byte[] VALID_PNG = new byte[] {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
	};
	private static final byte[] VALID_JPG = new byte[] {
			(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46
	};
	private static final byte[] VALID_PDF = new byte[] {
			0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34
	};
	private static final byte[] EXE_HEADER = new byte[] {
			0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00
	};

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;
	private final HttpClient http = HttpClient.newHttpClient();

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("""
				INSERT INTO users (full_name, email, password_hash, role, requester_type, area_id, active, created_at)
				VALUES ('Otra Solicitante', ?, ?, 'SOLICITANTE', 'ESTUDIANTE', NULL, TRUE, NOW(6))
				ON DUPLICATE KEY UPDATE active = TRUE
				""", OTHER_STUDENT, DEMO_HASH);
	}

	private String createRequestAs(String email) throws Exception {
		String token = api.login(email);
		String body = """
				{
					"categoryId": 1,
					"locationId": 3,
					"priorityId": 1,
					"description": "Fallo en el tomacorriente del aula 101"
				}
				""";
		HttpResponse<String> res = api.post("/api/v1/requests", body, token);
		assertThat(res.statusCode()).isEqualTo(201);
		return ApiClient.read(res, "$.code", String.class);
	}

	private HttpResponse<String> uploadMultipart(String code, String filename, byte[] content, String token) throws Exception {
		String boundary = "Boundary-" + UUID.randomUUID();
		String prefix = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
				+ "Content-Type: application/octet-stream\r\n\r\n";
		String suffix = "\r\n--" + boundary + "--\r\n";

		byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
		byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
		byte[] body = new byte[prefixBytes.length + content.length + suffixBytes.length];

		System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
		System.arraycopy(content, 0, body, prefixBytes.length, content.length);
		System.arraycopy(suffixBytes, 0, body, prefixBytes.length + content.length, suffixBytes.length);

		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/requests/" + code + "/evidences"))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body));

		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void uploadPngReturns201() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		HttpResponse<String> res = uploadMultipart(code, "captura.png", VALID_PNG, token);
		assertThat(res.statusCode()).isEqualTo(201);
		assertThat(ApiClient.read(res, "$.kind", String.class)).isEqualTo("IMG");
	}

	@Test
	void uploadJpgReturns201() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		HttpResponse<String> res = uploadMultipart(code, "foto.jpg", VALID_JPG, token);
		assertThat(res.statusCode()).isEqualTo(201);
		assertThat(ApiClient.read(res, "$.kind", String.class)).isEqualTo("IMG");
	}

	@Test
	void uploadPdfReturns201() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		HttpResponse<String> res = uploadMultipart(code, "documento.pdf", VALID_PDF, token);
		assertThat(res.statusCode()).isEqualTo(201);
		assertThat(ApiClient.read(res, "$.kind", String.class)).isEqualTo("PDF");
	}

	@Test
	void uploadSixthFileReturns400() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		for (int i = 0; i < 5; i++) {
			HttpResponse<String> r = uploadMultipart(code, "img" + i + ".png", VALID_PNG, token);
			assertThat(r.statusCode()).isEqualTo(201);
		}
		HttpResponse<String> sixth = uploadMultipart(code, "img6.png", VALID_PNG, token);
		assertThat(sixth.statusCode()).isEqualTo(400);
	}

	@Test
	void uploadFileOver5MbReturns400() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		byte[] large = new byte[(5 * 1024 * 1024) + 10];
		System.arraycopy(VALID_PNG, 0, large, 0, VALID_PNG.length);
		HttpResponse<String> res = uploadMultipart(code, "large.png", large, token);
		assertThat(res.statusCode()).isEqualTo(400);
	}

	@Test
	void uploadNonAllowedTypeReturns400() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		HttpResponse<String> res = uploadMultipart(code, "virus.exe", EXE_HEADER, token);
		assertThat(res.statusCode()).isEqualTo(400);
	}

	@Test
	void uploadExeRenamedToPngReturns400() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		HttpResponse<String> res = uploadMultipart(code, "disfrazado.png", EXE_HEADER, token);
		assertThat(res.statusCode()).isEqualTo(400);
	}

	@Test
	void uploadEmptyFileReturns400() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		HttpResponse<String> res = uploadMultipart(code, "vacio.png", new byte[0], token);
		assertThat(res.statusCode()).isEqualTo(400);
	}

	@Test
	void uploadWithoutTokenReturns401() throws Exception {
		String code = createRequestAs(STUDENT);
		HttpResponse<String> res = uploadMultipart(code, "foto.png", VALID_PNG, null);
		assertThat(res.statusCode()).isEqualTo(401);
	}

	@Test
	void downloadReturnsContentAndHeaders() throws Exception {
		String code = createRequestAs(STUDENT);
		String token = api.login(STUDENT);
		HttpResponse<String> uploadRes = uploadMultipart(code, "evidencia.png", VALID_PNG, token);
		int id = ApiClient.read(uploadRes, "$.id", Integer.class);

		HttpResponse<byte[]> res = http.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/requests/" + code + "/evidences/" + id))
				.header("Authorization", "Bearer " + token)
				.GET().build(), HttpResponse.BodyHandlers.ofByteArray());

		assertThat(res.statusCode()).isEqualTo(200);
		assertThat(res.headers().firstValue("Content-Type")).hasValue("image/png");
		assertThat(res.headers().firstValue("Content-Disposition")).hasValueSatisfying(cd -> assertThat(cd).contains("evidencia.png"));
		assertThat(res.body()).isEqualTo(VALID_PNG);
	}

	@Test
	void downloadWithoutPermissionReturns404() throws Exception {
		String code = createRequestAs(STUDENT);
		String studentToken = api.login(STUDENT);
		String strangerToken = api.login(OTHER_STUDENT);

		HttpResponse<String> uploadRes = uploadMultipart(code, "secreta.png", VALID_PNG, studentToken);
		int id = ApiClient.read(uploadRes, "$.id", Integer.class);

		HttpResponse<String> res = api.get("/api/v1/requests/" + code + "/evidences/" + id, strangerToken);
		assertThat(res.statusCode()).isEqualTo(404);
	}

	@Test
	void downloadWithWrongCodeReturns404() throws Exception {
		String code1 = createRequestAs(STUDENT);
		String code2 = createRequestAs(STUDENT);
		String token = api.login(STUDENT);

		HttpResponse<String> uploadRes = uploadMultipart(code1, "foto1.png", VALID_PNG, token);
		int id = ApiClient.read(uploadRes, "$.id", Integer.class);

		HttpResponse<String> res = api.get("/api/v1/requests/" + code2 + "/evidences/" + id, token);
		assertThat(res.statusCode()).isEqualTo(404);
	}

}
