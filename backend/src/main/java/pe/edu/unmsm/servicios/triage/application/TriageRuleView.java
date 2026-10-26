package pe.edu.unmsm.servicios.triage.application;

import java.util.List;

/** A rule as the administrator sees it, with the names of its category and priority. */
public record TriageRuleView(long id, List<String> keywords, long categoryId, String categoryName, long priorityId,
		String priorityName, boolean active) {

	public TriageRuleView {
		keywords = List.copyOf(keywords);
	}

}
