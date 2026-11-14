package pe.edu.unmsm.servicios.dashboard.domain;

import java.util.List;

/** The load of each technician of a team, from the open requests of their area. */
public final class TeamLoadCalculator {

	private static final String ASSIGNED = "ASIGNADA";
	private static final String IN_PROGRESS = "EN_ATENCION";

	private TeamLoadCalculator() {
	}

	/** One entry per technician, in the given order; requests of anybody else are ignored. */
	public static List<TechnicianLoad> calculate(List<TeamMember> technicians, List<TrackedRequest> open,
			int capacity) {
		return technicians.stream().map(technician -> {
			List<TrackedRequest> mine = open.stream()
					.filter(r -> r.open() && r.technicianId() != null && r.technicianId() == technician.id())
					.toList();
			return new TechnicianLoad(technician.id(), technician.fullName(), count(mine, ASSIGNED),
					count(mine, IN_PROGRESS), mine.stream().filter(TrackedRequest::overdue).count(), capacity);
		}).toList();
	}

	private static long count(List<TrackedRequest> requests, String status) {
		return requests.stream().filter(r -> status.equals(r.status())).count();
	}

}
