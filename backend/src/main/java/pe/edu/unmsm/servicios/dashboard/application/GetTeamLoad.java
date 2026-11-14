package pe.edu.unmsm.servicios.dashboard.application;

import java.time.Clock;
import java.util.List;

import pe.edu.unmsm.servicios.dashboard.domain.DashboardAccessDeniedException;
import pe.edu.unmsm.servicios.dashboard.domain.TeamLoadCalculator;
import pe.edu.unmsm.servicios.dashboard.domain.TeamMember;
import pe.edu.unmsm.servicios.dashboard.domain.TechnicianLoad;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsQuery;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.application.TechnicianDirectory;

/** {@code GET /dashboard/team}: the current load of the active technicians of the supervisor's area. */
public class GetTeamLoad {

	private static final String SUPERVISOR = "SUPERVISOR";

	private final TechnicianDirectory directory;
	private final RequestAnalyticsQuery analytics;
	private final Clock clock;
	private final int capacity;

	public GetTeamLoad(TechnicianDirectory directory, RequestAnalyticsQuery analytics, Clock clock, int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("The technician capacity must be at least 1");
		}
		this.directory = directory;
		this.analytics = analytics;
		this.clock = clock;
		this.capacity = capacity;
	}

	public List<TechnicianLoad> execute(AuthenticatedUser user) {
		if (!SUPERVISOR.equals(user.role()) || user.areaId() == null) {
			throw new DashboardAccessDeniedException("Solo un supervisor puede consultar la carga de su equipo");
		}
		long area = user.areaId();
		List<TeamMember> technicians = directory.activeTechniciansOfArea(area).stream()
				.map(t -> new TeamMember(t.id(), t.fullName())).toList();
		return TeamLoadCalculator.calculate(technicians,
				TrackedRequests.from(analytics.openRequests(area), clock.instant()), capacity);
	}

}
