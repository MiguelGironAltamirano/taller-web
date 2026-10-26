package pe.edu.unmsm.servicios.requests.application;

import java.util.EnumMap;
import java.util.Map;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** How many visible requests there are in total, overdue and per status; every status is present. */
public record RequestCountsView(long total, long overdue, Map<RequestStatus, Long> byStatus) {

	public RequestCountsView {
		Map<RequestStatus, Long> complete = new EnumMap<>(RequestStatus.class);
		for (RequestStatus status : RequestStatus.values()) {
			complete.put(status, byStatus == null ? 0L : byStatus.getOrDefault(status, 0L));
		}
		byStatus = Map.copyOf(complete);
	}

}
