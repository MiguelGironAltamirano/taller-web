package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;
import java.util.List;

/**
 * Read-only port for reporting (used by the dashboard module): flat rows of the requests and of their first
 * resolution, so other modules never read the request tables. It applies no visibility rule; callers decide the
 * area. A null {@code areaId} means every area.
 */
public interface RequestAnalyticsQuery {

	/** The requests registered in {@code [from, to)}. */
	List<RequestAnalyticsRow> registeredBetween(Long areaId, Instant from, Instant to);

	/** The requests that are neither RESUELTA nor CERRADA, whenever they were registered. */
	List<RequestAnalyticsRow> openRequests(Long areaId);

}
