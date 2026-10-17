package pe.edu.unmsm.servicios.notifications.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationTextTest {

	@Test
	void formatTransitionText() {
		String text = NotificationText.forTransition("SOL-2026-000001", "En atención");
		assertThat(text).isEqualTo("La solicitud SOL-2026-000001 pasó a En atención.");
	}

	@Test
	void formatAssignmentText() {
		String text = NotificationText.forAssignment("SOL-2026-000001");
		assertThat(text).isEqualTo("Se te asignó la solicitud SOL-2026-000001.");
	}

	@Test
	void formatReassignmentText() {
		String text = NotificationText.forReassignment("SOL-2026-000001");
		assertThat(text).isEqualTo("Se te reasignó la solicitud SOL-2026-000001.");
	}

	@Test
	void formatCommentText() {
		String text = NotificationText.forComment("Carlos Gómez", "SOL-2026-000001");
		assertThat(text).isEqualTo("Carlos Gómez comentó en la solicitud SOL-2026-000001.");
	}

}
