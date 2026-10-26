package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The resolved requests still waiting for the requester's answer, for the automatic closure (workflow). The waiting
 * time counts from the moment the request last entered {@code RESUELTA} according to its history, not from its
 * creation: a solution rejected and resolved again starts a new period.
 */
public interface UnconfirmedResolutions {

	/**
	 * Codes of the requests that are {@code RESUELTA} and entered that status at or before {@code cutoff}, the
	 * longest waiting first, at most {@code limit}.
	 */
	List<String> resolvedAtOrBefore(Instant cutoff, int limit);

	/** When the request last entered {@code RESUELTA}; empty if it never did. */
	Optional<Instant> resolvedAt(String code);

}
