package pe.edu.unmsm.servicios.triage.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.triage.domain.TriageMatcher;
import pe.edu.unmsm.servicios.triage.domain.TriageMatcher.RuleMatch;

/**
 * Category and priority suggestion while the requester writes the description (spec §5.1). Only active rules whose
 * category is still active take part; nothing is stored.
 */
public class SuggestTriage {

	private final TriageRuleStore rules;
	private final CatalogLookup catalog;

	public SuggestTriage(TriageRuleStore rules, CatalogLookup catalog) {
		this.rules = rules;
		this.catalog = catalog;
	}

	@Transactional(readOnly = true)
	public Optional<TriageSuggestion> suggest(String description) {
		Map<Long, Boolean> activeCategories = new HashMap<>();
		return TriageMatcher.rank(description, rules.findActive()).stream()
				.map(RuleMatch::rule)
				.filter(rule -> activeCategories.computeIfAbsent(rule.categoryId(),
						id -> catalog.findActiveCategory(id).isPresent()))
				.findFirst()
				.map(rule -> new TriageSuggestion(rule.categoryId(), rule.priorityId()));
	}

}
