package pe.edu.unmsm.servicios.requests.application;

import java.util.Optional;

import pe.edu.unmsm.servicios.requests.domain.RequestDraft;

/** The one draft each requester may have. */
public interface RequestDraftRepository {

	Optional<RequestDraft> find(long userId);

	/** Creates or replaces the draft of the user. */
	void save(long userId, RequestDraft draft);

	void delete(long userId);

}
