package pe.edu.unmsm.servicios.triage.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.triage.domain.InvalidTriageRuleException;
import pe.edu.unmsm.servicios.triage.domain.TriageKeywords;
import pe.edu.unmsm.servicios.triage.domain.TriageRule;
import pe.edu.unmsm.servicios.triage.domain.TriageRuleNotFoundException;

/**
 * Administration of the triage rules. The category must be active and the priority must exist; all the field
 * errors of a body are reported together.
 */
public class ManageTriageRules {

	private final TriageRuleStore rules;
	private final CatalogLookup catalog;

	public ManageTriageRules(TriageRuleStore rules, CatalogLookup catalog) {
		this.rules = rules;
		this.catalog = catalog;
	}

	@Transactional(readOnly = true)
	public List<TriageRuleView> list() {
		return rules.findAll();
	}

	@Transactional
	public TriageRuleView create(TriageRuleCommand command) {
		long id = rules.insert(validated(0, command));
		return rules.find(id).orElseThrow();
	}

	@Transactional
	public TriageRuleView update(long id, TriageRuleCommand command) {
		if (!rules.exists(id)) {
			throw new TriageRuleNotFoundException();
		}
		rules.update(validated(id, command));
		return rules.find(id).orElseThrow();
	}

	@Transactional
	public void delete(long id) {
		if (!rules.exists(id)) {
			throw new TriageRuleNotFoundException();
		}
		rules.delete(id);
	}

	private TriageRule validated(long id, TriageRuleCommand command) {
		Map<String, String> errors = new LinkedHashMap<>();
		List<String> keywords = List.of();
		try {
			keywords = TriageKeywords.clean(command.keywords());
		}
		catch (InvalidTriageRuleException ex) {
			errors.putAll(ex.errors());
		}
		if (command.categoryId() == null || catalog.findActiveCategory(command.categoryId()).isEmpty()) {
			errors.put("categoryId", "La categoría no existe o está inactiva");
		}
		if (command.priorityId() == null || catalog.findPriority(command.priorityId()).isEmpty()) {
			errors.put("priorityId", "La prioridad no existe");
		}
		if (!errors.isEmpty()) {
			throw new InvalidTriageRuleException(errors);
		}
		boolean active = command.active() == null || command.active();
		return new TriageRule(id, keywords, command.categoryId(), command.priorityId(), active);
	}

}
