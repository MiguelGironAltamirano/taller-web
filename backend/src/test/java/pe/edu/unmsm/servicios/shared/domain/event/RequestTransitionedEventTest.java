package pe.edu.unmsm.servicios.shared.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestTransitionedEventTest {

	private static RequestTransitionedEvent event(String fromStatus, String toStatus) {
		return new RequestTransitionedEvent("SOL-2026-000001", 1L, 4L, 1L, 3L, 2L, fromStatus, toStatus,
				"ASIGNADA".equals(toStatus));
	}

	@Test
	void assignmentFromEvaluationIsNotAReassignment() {
		assertThat(event("EN_EVALUACION", "ASIGNADA").isReassignment()).isFalse();
	}

	@Test
	void assignmentFromAssignedIsAReassignment() {
		assertThat(event("ASIGNADA", "ASIGNADA").isReassignment()).isTrue();
	}

	@Test
	void leavingAssignedIsNotAReassignment() {
		assertThat(event("ASIGNADA", "EN_ATENCION").isReassignment()).isFalse();
	}

}
