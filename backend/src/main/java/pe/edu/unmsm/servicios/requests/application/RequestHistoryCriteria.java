package pe.edu.unmsm.servicios.requests.application;

import java.util.Set;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Filter for {@link RequestHistoryQuery}. Every change matches when {@code text} is blank and {@code statuses}
 * is empty; otherwise a change matches when {@code text} appears in the actor name, request code or reason
 * (case-insensitive), or when its origin or target status is one of {@code statuses}.
 */
public record RequestHistoryCriteria(String text, Set<RequestStatus> statuses, int limit) {

	public RequestHistoryCriteria {
		text = text == null ? "" : text.strip();
		statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be greater than 0");
		}
	}

	public boolean matchesEverything() {
		return text.isEmpty() && statuses.isEmpty();
	}

}
