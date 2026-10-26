package pe.edu.unmsm.servicios.triage.application;

import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.triage.domain.TriageRule;

/** Persistence of the triage rules. Keywords are stored in the order they were given. */
public interface TriageRuleStore {

	/** All rules ordered by id. */
	List<TriageRuleView> findAll();

	Optional<TriageRuleView> find(long id);

	/** The active rules, whatever the state of their category. */
	List<TriageRule> findActive();

	boolean exists(long id);

	/** Stores a new rule ({@code rule.id()} is ignored) and returns its id. */
	long insert(TriageRule rule);

	void update(TriageRule rule);

	void delete(long id);

}
