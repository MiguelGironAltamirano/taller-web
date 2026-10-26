package pe.edu.unmsm.servicios.triage.infrastructure.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.triage.application.ManageTriageRules;
import pe.edu.unmsm.servicios.triage.application.TriageRuleCommand;
import pe.edu.unmsm.servicios.triage.application.TriageRuleView;

/** Field validation lives in {@link ManageTriageRules} so every error of a body is reported in one response. */
@RestController
@RequestMapping("/api/v1/admin/triage-rules")
@PreAuthorize("hasRole('ADMINISTRADOR')")
class AdminTriageRulesController {

	private final ManageTriageRules rules;

	AdminTriageRulesController(ManageTriageRules rules) {
		this.rules = rules;
	}

	@GetMapping
	List<TriageRuleResponse> list() {
		return rules.list().stream().map(TriageRuleResponse::from).toList();
	}

	@PostMapping
	ResponseEntity<TriageRuleResponse> create(@RequestBody TriageRuleBody body) {
		return ResponseEntity.status(HttpStatus.CREATED).body(TriageRuleResponse.from(rules.create(body.toCommand())));
	}

	@PutMapping("/{id}")
	TriageRuleResponse update(@PathVariable long id, @RequestBody TriageRuleBody body) {
		return TriageRuleResponse.from(rules.update(id, body.toCommand()));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable long id) {
		rules.delete(id);
		return ResponseEntity.noContent().build();
	}

	record TriageRuleBody(List<String> keywords, Long categoryId, Long priorityId, Boolean active) {

		TriageRuleCommand toCommand() {
			return new TriageRuleCommand(keywords, categoryId, priorityId, active);
		}

	}

	record TriageRuleResponse(long id, List<String> keywords, long categoryId, String categoryName, long priorityId,
			String priorityName, boolean active) {

		static TriageRuleResponse from(TriageRuleView rule) {
			return new TriageRuleResponse(rule.id(), rule.keywords(), rule.categoryId(), rule.categoryName(),
					rule.priorityId(), rule.priorityName(), rule.active());
		}

	}

}
