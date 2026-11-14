package pe.edu.unmsm.servicios.triage.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.Catalogs;
import pe.edu.unmsm.servicios.catalog.domain.Category;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;
import pe.edu.unmsm.servicios.triage.domain.TriageRule;

class SuggestTriageTest {

	private static final TriageRule RULE = new TriageRule(1, List.of("goteo"), 2, 3, true);

	private final TriageRuleStore rules = new TriageRuleStore() {

		@Override
		public List<TriageRuleView> findAll() {
			return List.of();
		}

		@Override
		public Optional<TriageRuleView> find(long id) {
			return Optional.empty();
		}

		@Override
		public List<TriageRule> findActive() {
			return List.of(RULE);
		}

		@Override
		public boolean exists(long id) {
			return false;
		}

		@Override
		public long insert(TriageRule rule) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void update(TriageRule rule) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void delete(long id) {
			throw new UnsupportedOperationException();
		}

	};

	private final CatalogLookup catalog = new CatalogLookup() {

		@Override
		public Catalogs activeCatalogs() {
			return new Catalogs(List.of(), List.of(), List.of(), List.of());
		}

		@Override
		public Optional<Category> findActiveCategory(long id) {
			return Optional.of(new Category(id, "Gasfitería", 1));
		}

		@Override
		public Optional<PriorityLevel> findPriority(long id) {
			return Optional.empty();
		}

		@Override
		public Optional<Location> findLocation(long id) {
			return Optional.empty();
		}

	};

	@Test
	void aMatchingRuleIsSuggestedWhileSuggestionsAreEnabled() {
		SuggestTriage useCase = new SuggestTriage(rules, catalog, () -> new SystemSettings(72, false, true));

		assertThat(useCase.suggest("Hay goteo en el baño")).contains(new TriageSuggestion(2, 3));
	}

	@Test
	void nothingIsSuggestedWhenTheAdministratorDisabledSuggestions() {
		SuggestTriage useCase = new SuggestTriage(rules, catalog, () -> new SystemSettings(72, false, false));

		assertThat(useCase.suggest("Hay goteo en el baño")).isEmpty();
	}

}
