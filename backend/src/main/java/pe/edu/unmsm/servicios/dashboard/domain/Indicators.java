package pe.edu.unmsm.servicios.dashboard.domain;

import java.util.List;

/** Everything the dashboard shows for a scope and a period ({@code DashboardIndicators} of the contract). */
public record Indicators(int rangeDays, String areaName, Kpis kpis, List<Tally> byCategory, List<Tally> byPriority,
		List<Tally> byTechnician, List<Long> timeline, double slaCompliancePct, List<TrackedRequest> dueSoon) {

	public Indicators {
		byCategory = List.copyOf(byCategory);
		byPriority = List.copyOf(byPriority);
		byTechnician = List.copyOf(byTechnician);
		timeline = List.copyOf(timeline);
		dueSoon = List.copyOf(dueSoon);
	}

}
