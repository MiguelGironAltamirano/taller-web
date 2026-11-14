package pe.edu.unmsm.servicios.notifications.domain;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationRecipientsTest {

	@Test
	void statusChangeNotifiesRequesterTechnicianAndSupervisorsExcludingActor() {
		// Solicitante: 10, Técnico: 20, Supervisores: [30, 31], Actor: 30 (uno de los supervisores)
		List<Long> supervisors = List.of(30L, 31L);
		List<Long> recipients = NotificationRecipients.forTransition(10L, 20L, supervisors, 30L);

		assertThat(recipients).containsExactlyInAnyOrder(10L, 20L, 31L);
	}

	@Test
	void statusChangeWithoutTechnicianExcludesNull() {
		List<Long> supervisors = List.of(30L);
		List<Long> recipients = NotificationRecipients.forTransition(10L, null, supervisors, 10L);

		// Solicitante es el actor, no hay técnico, solo queda el supervisor
		assertThat(recipients).containsExactly(30L);
	}

	@Test
	void aChangeMadeByTheSystemNotifiesEveryParticipant() {
		List<Long> recipients = NotificationRecipients.forTransition(10L, 20L, List.of(30L, 31L), null);

		assertThat(recipients).containsExactly(10L, 20L, 30L, 31L);
	}

	@Test
	void statusChangeOfAnAssignmentLeavesOutTheAssignedTechnician() {
		// The assigned technician already gets the specific assignment notification
		List<Long> recipients = NotificationRecipients.forAssignmentTransition(10L, 20L, List.of(30L, 31L), 30L);

		assertThat(recipients).containsExactly(10L, 31L);
	}

	@Test
	void assignmentNotifiesOnlyNewTechnicianExcludingSupervisor() {
		List<Long> recipients = NotificationRecipients.forAssignment(25L, 2L);
		assertThat(recipients).containsExactly(25L);
	}

	@Test
	void commentNotifiesRequesterTechnicianAndSupervisorsExcludingAuthor() {
		List<Long> supervisors = List.of(30L, 31L);
		List<Long> recipients = NotificationRecipients.forComment(10L, 20L, supervisors, 10L);

		assertThat(recipients).containsExactlyInAnyOrder(20L, 30L, 31L);
	}

}
