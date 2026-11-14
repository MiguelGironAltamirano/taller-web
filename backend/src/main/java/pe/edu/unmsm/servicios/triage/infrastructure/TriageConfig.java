package pe.edu.unmsm.servicios.triage.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.shared.application.SystemSettingsProvider;
import pe.edu.unmsm.servicios.triage.application.ManageTriageRules;
import pe.edu.unmsm.servicios.triage.application.SuggestTriage;
import pe.edu.unmsm.servicios.triage.application.TriageRuleStore;

@Configuration
class TriageConfig {

	@Bean
	SuggestTriage suggestTriage(TriageRuleStore rules, CatalogLookup catalog, SystemSettingsProvider settings) {
		return new SuggestTriage(rules, catalog, settings);
	}

	@Bean
	ManageTriageRules manageTriageRules(TriageRuleStore rules, CatalogLookup catalog) {
		return new ManageTriageRules(rules, catalog);
	}

}
