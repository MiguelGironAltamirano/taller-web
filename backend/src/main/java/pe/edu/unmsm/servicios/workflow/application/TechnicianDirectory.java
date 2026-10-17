package pe.edu.unmsm.servicios.workflow.application;

import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.workflow.domain.Technician;

/** The technicians a request can be assigned to; backed by the user accounts of the auth module. */
public interface TechnicianDirectory {

	/** The user with that id, only if it is a technician (active or not). */
	Optional<Technician> findTechnician(long id);

	/** Active technicians of the area, ordered by name. */
	List<Technician> activeTechniciansOfArea(long areaId);

}
