package pe.edu.unmsm.servicios.workflow.application;

import java.util.List;
import java.util.Map;

import pe.edu.unmsm.servicios.requests.application.TechnicianWorkload;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.domain.Technician;
import pe.edu.unmsm.servicios.workflow.domain.TransitionForbiddenException;

public class ListTechnicians {

	private static final String SUPERVISOR = "SUPERVISOR";

	private final TechnicianDirectory directory;
	private final TechnicianWorkload workload;

	public ListTechnicians(TechnicianDirectory directory, TechnicianWorkload workload) {
		this.directory = directory;
		this.workload = workload;
	}

	/**
	 * The active technicians of an area. Only supervisors may ask, and only for their own area (which is the
	 * default when none is given).
	 */
	public List<TechnicianView> execute(AuthenticatedUser user, Long areaId) {
		if (!SUPERVISOR.equals(user.role()) || user.areaId() == null) {
			throw new TransitionForbiddenException("Solo un supervisor puede consultar los técnicos de su área");
		}
		long area = areaId == null ? user.areaId() : areaId;
		if (area != user.areaId()) {
			throw new TransitionForbiddenException("Solo puedes consultar los técnicos de tu propia área");
		}
		List<Technician> technicians = directory.activeTechniciansOfArea(area);
		Map<Long, Long> load = workload.activeRequestsByTechnician(technicians.stream().map(Technician::id).toList());
		return technicians.stream()
				.map(t -> new TechnicianView(t.id(), t.fullName(), load.getOrDefault(t.id(), 0L)))
				.toList();
	}

}
