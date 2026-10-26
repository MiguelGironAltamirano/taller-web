package pe.edu.unmsm.servicios.requests.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

class RequestAnalyticsRowTest {

	private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

	private static RequestAnalyticsRow row(RequestStatus status, Instant firstResolvedAt) {
		return new RequestAnalyticsRow("SOL-2026-000001", status, "Electricidad", "Alta", 1, "Mantenimiento", 3L,
				"Técnico", "Aula", T0, T0.plusSeconds(3600), firstResolvedAt);
	}

	@Test
	void attendedMeansItReachedResueltaAndStillIsResolvedOrClosed() {
		assertThat(row(RequestStatus.RESUELTA, T0).wasAttended()).isTrue();
		assertThat(row(RequestStatus.CERRADA, T0).wasAttended()).isTrue();
		// Reopened after the resolution: it is pending again.
		assertThat(row(RequestStatus.EN_ATENCION, T0).wasAttended()).isFalse();
		// Closed as NO_PROCEDE or DUPLICADA: never resolved.
		assertThat(row(RequestStatus.CERRADA, null).wasAttended()).isFalse();
	}

	@Test
	void openAndOverdueFollowTheSlaRuleOfTheRequestList() {
		assertThat(row(RequestStatus.ASIGNADA, null).isOpen()).isTrue();
		assertThat(row(RequestStatus.RESUELTA, T0).isOpen()).isFalse();
		assertThat(row(RequestStatus.ASIGNADA, null).isOverdue(T0.plusSeconds(3601))).isTrue();
		assertThat(row(RequestStatus.ASIGNADA, null).isOverdue(T0.plusSeconds(3600))).isFalse();
		assertThat(row(RequestStatus.RESUELTA, T0).isOverdue(T0.plusSeconds(7200))).isFalse();
	}

}
