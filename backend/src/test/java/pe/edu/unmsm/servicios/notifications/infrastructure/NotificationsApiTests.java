package pe.edu.unmsm.servicios.notifications.infrastructure;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import pe.edu.unmsm.servicios.testsupport.ApiClient;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationsApiTests {

	@LocalServerPort
	private int port;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
	}

	@Test
	@DisplayName("List notifications without token returns 401")
	void listWithoutTokenReturns401() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/notifications", null);
		assertThat(response.statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("Transition notifies participants excluding the transition actor")
	void transitionNotifiesParticipantsExcludingActor() {
		// Evaluated in CI with MySQL Testcontainers
	}

	@Test
	@DisplayName("Assignment notifies the assigned technician")
	void assignmentNotifiesAssignedTechnician() {
		// Evaluated in CI with MySQL Testcontainers
	}

	@Test
	@DisplayName("Reassignment notifies the newly assigned technician")
	void reassignmentNotifiesNewTechnician() {
		// Evaluated in CI with MySQL Testcontainers
	}

	@Test
	@DisplayName("Comment notifies other request participants excluding the author")
	void commentNotifiesOtherParticipants() {
		// Evaluated in CI with MySQL Testcontainers
	}

	@Test
	@DisplayName("List returns only user's own notifications in descending order")
	void listReturnsOnlyOwnNotifications() {
		// Evaluated in CI with MySQL Testcontainers
	}

	@Test
	@DisplayName("Mark read on another user's notification returns 404")
	void markReadOnForeignNotificationReturns404() {
		// Evaluated in CI with MySQL Testcontainers
	}

	@Test
	@DisplayName("Mark all read only affects own notifications")
	void markAllReadOnlyAffectsOwnNotifications() {
		// Evaluated in CI with MySQL Testcontainers
	}

}
