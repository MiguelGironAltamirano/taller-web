package pe.edu.unmsm.servicios.requests.application;

import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;

public interface RequestRepository {

	/** Persists a new request together with its first history entry, atomically. */
	ServiceRequest save(ServiceRequest request, RequestHistoryEntry firstEntry);

}
