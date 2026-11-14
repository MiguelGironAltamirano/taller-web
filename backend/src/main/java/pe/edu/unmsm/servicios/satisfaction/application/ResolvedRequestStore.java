package pe.edu.unmsm.servicios.satisfaction.application;

import java.time.Instant;
import java.util.List;

public interface ResolvedRequestStore {

	/** Requests still in Resuelta whose latest move to Resuelta happened at or before {@code resolvedBefore}. */
	List<ResolvedRequest> findResolvedBefore(Instant resolvedBefore);

	/**
	 * Closes the request with closure reason AUTOMATICA and a history entry without a human actor, in one
	 * transaction. Returns false when the request is no longer in Resuelta (the requester answered meanwhile).
	 */
	boolean closeAutomatically(long requestId, Instant now);

	record ResolvedRequest(long id, String code, long requesterId) {
	}

}
