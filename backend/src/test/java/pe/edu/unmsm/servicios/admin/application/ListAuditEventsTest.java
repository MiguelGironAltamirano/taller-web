package pe.edu.unmsm.servicios.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.admin.domain.AuditEvent;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryCriteria;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryQuery;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryRecord;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

class ListAuditEventsTest {

	private static final Instant AT = Instant.parse("2026-10-20T15:00:00Z");

	private final RecordingHistoryQuery history = new RecordingHistoryQuery();
	private final ListAuditEvents audit = new ListAuditEvents(history);

	@Test
	void describesEachChangeWithStateLabelsAndReason() {
		history.result = List.of(
				new RequestHistoryRecord(2, AT, "Carlos Supervisor", "SOL-2026-0001", RequestStatus.REGISTRADA,
						RequestStatus.EN_EVALUACION, "Revisión inicial"),
				new RequestHistoryRecord(1, AT.minusSeconds(60), "Estudiante Demo", "SOL-2026-0001", null,
						RequestStatus.REGISTRADA, null));

		List<AuditEvent> events = audit.execute(null);

		assertThat(events).containsExactly(
				new AuditEvent(2, AT, "Carlos Supervisor", "SOL-2026-0001", "Registrada → En evaluación · Revisión inicial"),
				new AuditEvent(1, AT.minusSeconds(60), "Estudiante Demo", "SOL-2026-0001", "— → Registrada"));
	}

	@Test
	void asksForTheLatestFortyChangesWhenThereIsNoSearchText() {
		audit.execute("   ");

		assertThat(history.criteria).isEqualTo(new RequestHistoryCriteria("", Set.of(), 40));
	}

	@Test
	void searchTextAlsoMatchesStatesByTheirLabelIgnoringCaseAndAccents() {
		audit.execute(" EVALUACION ");

		assertThat(history.criteria.text()).isEqualTo("EVALUACION");
		assertThat(history.criteria.statuses()).containsExactly(RequestStatus.EN_EVALUACION);
		assertThat(history.criteria.limit()).isEqualTo(40);
	}

	@Test
	void searchTextMatchingNoStateLabelSearchesOnlyText() {
		audit.execute("SOL-2026");

		assertThat(history.criteria.statuses()).isEmpty();
	}

	private static final class RecordingHistoryQuery implements RequestHistoryQuery {

		private List<RequestHistoryRecord> result = List.of();
		private RequestHistoryCriteria criteria;

		@Override
		public List<RequestHistoryRecord> latest(RequestHistoryCriteria criteria) {
			this.criteria = criteria;
			return result;
		}

	}

}
