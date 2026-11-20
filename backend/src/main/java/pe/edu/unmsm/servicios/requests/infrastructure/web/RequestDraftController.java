package pe.edu.unmsm.servicios.requests.infrastructure.web;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.requests.application.ManageRequestDraft;
import pe.edu.unmsm.servicios.requests.domain.RequestDraft;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

/** The requester's draft of a new request (docs/api/contrato-v1.md «Borrador de nueva solicitud»). */
@RestController
@RequestMapping("/api/v1/me/request-draft")
@PreAuthorize("hasRole('SOLICITANTE')")
class RequestDraftController {

	record DraftBody(String category, String description, String priority, String faculty, String pavilion,
			String room) {
	}

	record DraftResponse(String category, String description, String priority, String faculty, String pavilion,
			String room, Instant updatedAt) {

		static DraftResponse from(RequestDraft draft) {
			return new DraftResponse(draft.category(), draft.description(), draft.priority(), draft.faculty(),
					draft.pavilion(), draft.room(), draft.updatedAt());
		}

	}

	private final ManageRequestDraft drafts;

	RequestDraftController(ManageRequestDraft drafts) {
		this.drafts = drafts;
	}

	@GetMapping
	ResponseEntity<DraftResponse> get(@AuthenticationPrincipal Jwt jwt) {
		return drafts.get(CurrentUser.from(jwt)).map(draft -> ResponseEntity.ok(DraftResponse.from(draft)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PutMapping
	ResponseEntity<DraftResponse> save(@RequestBody DraftBody body, @AuthenticationPrincipal Jwt jwt) {
		return drafts.save(CurrentUser.from(jwt), new ManageRequestDraft.Fields(body.category(), body.description(),
				body.priority(), body.faculty(), body.pavilion(), body.room()))
				.map(draft -> ResponseEntity.ok(DraftResponse.from(draft)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@DeleteMapping
	ResponseEntity<Void> discard(@AuthenticationPrincipal Jwt jwt) {
		drafts.discard(CurrentUser.from(jwt));
		return ResponseEntity.noContent().build();
	}

}
