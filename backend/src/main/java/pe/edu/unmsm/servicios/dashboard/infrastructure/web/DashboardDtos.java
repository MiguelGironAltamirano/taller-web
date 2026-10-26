package pe.edu.unmsm.servicios.dashboard.infrastructure.web;

import java.time.Instant;
import java.util.List;

import pe.edu.unmsm.servicios.dashboard.domain.Indicators;
import pe.edu.unmsm.servicios.dashboard.domain.Kpi;
import pe.edu.unmsm.servicios.dashboard.domain.Kpis;
import pe.edu.unmsm.servicios.dashboard.domain.Tally;
import pe.edu.unmsm.servicios.dashboard.domain.TechnicianLoad;
import pe.edu.unmsm.servicios.dashboard.domain.TrackedRequest;

/** The shapes of {@code docs/api/contrato-v1.md}, section "Dashboard (indicadores)". */
final class DashboardDtos {

	private DashboardDtos() {
	}

	record IndicatorsResponse(int rangeDays, String areaName, KpisResponse kpis, List<LabelValue> byCategory,
			List<LabelValue> byPriority, List<LabelValue> byTechnician, List<Long> timeline, double slaCompliancePct,
			List<RequestSummaryResponse> dueSoon) {

		static IndicatorsResponse from(Indicators i) {
			return new IndicatorsResponse(i.rangeDays(), i.areaName(), KpisResponse.from(i.kpis()),
					LabelValue.from(i.byCategory()), LabelValue.from(i.byPriority()), LabelValue.from(i.byTechnician()),
					i.timeline(), i.slaCompliancePct(), i.dueSoon().stream().map(RequestSummaryResponse::from).toList());
		}

	}

	record KpisResponse(KpiResponse registered, KpiResponse pending, KpiResponse attended, KpiResponse resolvedPct,
			KpiResponse avgAttentionHours, KpiResponse overdue) {

		static KpisResponse from(Kpis k) {
			return new KpisResponse(KpiResponse.from(k.registered()), KpiResponse.from(k.pending()),
					KpiResponse.from(k.attended()), KpiResponse.from(k.resolvedPct()),
					KpiResponse.from(k.avgAttentionHours()), KpiResponse.from(k.overdue()));
		}

	}

	record KpiResponse(double value, double deltaPct, List<Double> series) {

		static KpiResponse from(Kpi k) {
			return new KpiResponse(k.value(), k.deltaPct(), k.series());
		}

	}

	record LabelValue(String label, long value) {

		static List<LabelValue> from(List<Tally> tallies) {
			return tallies.stream().map(t -> new LabelValue(t.label(), t.value())).toList();
		}

	}

	/** {@code RequestSummary} of the contract. */
	record RequestSummaryResponse(String code, String categoryName, String locationName, String priorityName,
			String status, Instant createdAt, Instant dueAt, boolean overdue) {

		static RequestSummaryResponse from(TrackedRequest r) {
			return new RequestSummaryResponse(r.code(), r.categoryName(), r.locationName(), r.priorityName(),
					r.status(), r.createdAt(), r.dueAt(), r.overdue());
		}

	}

	record TeamMemberResponse(long id, String fullName, long assigned, long inProgress, long overdue, int capacity) {

		static TeamMemberResponse from(TechnicianLoad t) {
			return new TeamMemberResponse(t.id(), t.fullName(), t.assigned(), t.inProgress(), t.overdue(),
					t.capacity());
		}

	}

}
