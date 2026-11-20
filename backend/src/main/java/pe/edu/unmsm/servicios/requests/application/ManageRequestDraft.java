package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.domain.RequestDraft;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/** Keeps the progress of the new request form so the requester can leave and pick it up later, on any device. */
public class ManageRequestDraft {

	public record Fields(String category, String description, String priority, String faculty, String pavilion,
			String room) {
	}

	private final RequestDraftRepository drafts;
	private final Clock clock;

	public ManageRequestDraft(RequestDraftRepository drafts, Clock clock) {
		this.drafts = drafts;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Optional<RequestDraft> get(AuthenticatedUser requester) {
		return drafts.find(requester.id());
	}

	/** Saves the draft; an empty one is discarded instead, so there is nothing to offer later. */
	@Transactional
	public Optional<RequestDraft> save(AuthenticatedUser requester, Fields fields) {
		RequestDraft draft = RequestDraft.of(fields.category(), fields.description(), fields.priority(),
				fields.faculty(), fields.pavilion(), fields.room(), clock.instant());
		if (draft.isEmpty()) {
			drafts.delete(requester.id());
			return Optional.empty();
		}
		drafts.save(requester.id(), draft);
		return Optional.of(draft);
	}

	@Transactional
	public void discard(AuthenticatedUser requester) {
		drafts.delete(requester.id());
	}

}
