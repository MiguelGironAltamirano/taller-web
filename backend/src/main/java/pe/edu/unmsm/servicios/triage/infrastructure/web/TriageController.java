package pe.edu.unmsm.servicios.triage.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.triage.application.SuggestTriage;

@RestController
@RequestMapping("/api/v1/triage")
@PreAuthorize("hasRole('SOLICITANTE')")
class TriageController {

	/**
	 * The contract answers {@code 200} with a JSON {@code null} body when no rule matches. A null body would be
	 * written as an empty response, so the JSON literal is sent as is.
	 */
	private static final String NO_SUGGESTION = "null";

	private final SuggestTriage suggestTriage;

	TriageController(SuggestTriage suggestTriage) {
		this.suggestTriage = suggestTriage;
	}

	@PostMapping("/suggestions")
	ResponseEntity<?> suggest(@Valid @RequestBody SuggestionRequest request) {
		return suggestTriage.suggest(request.description())
				.<ResponseEntity<?>>map(suggestion -> ResponseEntity.ok(
						new SuggestionResponse(suggestion.categoryId(), suggestion.priorityId())))
				.orElseGet(() -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NO_SUGGESTION));
	}

	record SuggestionRequest(
			@NotBlank(message = "La descripción es obligatoria")
			@Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres") String description) {
	}

	record SuggestionResponse(long categoryId, long priorityId) {
	}

}
