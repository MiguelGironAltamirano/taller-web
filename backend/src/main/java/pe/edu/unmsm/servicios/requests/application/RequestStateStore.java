package pe.edu.unmsm.servicios.requests.application;

import java.util.Optional;

import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;

/**
 * Access to the state of an existing request for the modules that change it (workflow). The request is read
 * with a write lock and updated in the same transaction, so two transitions of one request never interleave.
 */
public interface RequestStateStore {

	/** Reads the request and locks it until the surrounding transaction ends. */
	Optional<ServiceRequest> findByCodeForUpdate(String code);

	/** Saves the new status, technician and closure reason together with the history entry of the change. */
	ServiceRequest update(ServiceRequest request, RequestHistoryEntry entry);

}
