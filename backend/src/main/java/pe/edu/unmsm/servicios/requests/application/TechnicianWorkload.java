package pe.edu.unmsm.servicios.requests.application;

import java.util.Collection;
import java.util.Map;

/** How much work each technician currently has. */
public interface TechnicianWorkload {

	/** Requests assigned or in attention per technician; technicians without any are absent from the map. */
	Map<Long, Long> activeRequestsByTechnician(Collection<Long> technicianIds);

}
