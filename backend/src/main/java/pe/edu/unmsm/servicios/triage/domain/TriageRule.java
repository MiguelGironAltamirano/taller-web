package pe.edu.unmsm.servicios.triage.domain;

import java.util.List;

/** A keyword rule that suggests a category and a priority (spec §5.1). Keywords are already clean. */
public record TriageRule(long id, List<String> keywords, long categoryId, long priorityId, boolean active) {

	public TriageRule {
		keywords = List.copyOf(keywords);
	}

}
