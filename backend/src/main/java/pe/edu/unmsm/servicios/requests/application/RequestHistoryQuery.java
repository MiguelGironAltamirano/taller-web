package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

/** Read-only port over the change history of every request (used by the administration audit log). */
public interface RequestHistoryQuery {

	/** Changes matching {@code criteria}, newest first, at most {@code criteria.limit()} of them. */
	List<RequestHistoryRecord> latest(RequestHistoryCriteria criteria);

}
