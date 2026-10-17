package pe.edu.unmsm.servicios.requests.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SlaTest {

	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");
	private static final Instant PAST = NOW.minusSeconds(3600);
	private static final Instant FUTURE = NOW.plusSeconds(3600);

	@Test
	void openRequestPastItsDueDateIsOverdue() {
		for (RequestStatus status : new RequestStatus[] { RequestStatus.REGISTRADA, RequestStatus.EN_EVALUACION,
				RequestStatus.ASIGNADA, RequestStatus.EN_ATENCION }) {
			assertThat(Sla.isOverdue(status, PAST, NOW)).as(status.name()).isTrue();
		}
	}

	@Test
	void requestThatReachedResueltaOrWasClosedIsNeverOverdue() {
		assertThat(Sla.isOverdue(RequestStatus.RESUELTA, PAST, NOW)).isFalse();
		assertThat(Sla.isOverdue(RequestStatus.CERRADA, PAST, NOW)).isFalse();
	}

	@Test
	void openRequestBeforeItsDueDateIsNotOverdue() {
		assertThat(Sla.isOverdue(RequestStatus.EN_ATENCION, FUTURE, NOW)).isFalse();
	}

	@Test
	void requestDueExactlyNowIsNotYetOverdue() {
		assertThat(Sla.isOverdue(RequestStatus.ASIGNADA, NOW, NOW)).isFalse();
	}

}
