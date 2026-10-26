package pe.edu.unmsm.servicios.triage.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Keyword rules of a triage rule: 1 to 20 words or phrases of 2 to 50 characters, stored lowercase, without
 * extra spaces and without repeats. Two keywords that only differ in accents are the same keyword, because
 * they match exactly the same descriptions.
 */
public final class TriageKeywords {

	public static final int MAX_KEYWORDS = 20;
	public static final int MIN_LENGTH = 2;
	public static final int MAX_LENGTH = 50;

	private TriageKeywords() {
	}

	public static List<String> clean(List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			throw invalid("Ingresa al menos una palabra clave");
		}
		List<String> keywords = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String keyword : raw) {
			String tidy = keyword == null ? "" : TriageText.tidy(keyword).toLowerCase(Locale.ROOT);
			if (tidy.length() < MIN_LENGTH || tidy.length() > MAX_LENGTH) {
				throw invalid("Cada palabra clave debe tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres");
			}
			if (seen.add(TriageText.normalize(tidy))) {
				keywords.add(tidy);
			}
		}
		if (keywords.size() > MAX_KEYWORDS) {
			throw invalid("Se permiten como máximo " + MAX_KEYWORDS + " palabras clave");
		}
		return List.copyOf(keywords);
	}

	private static InvalidTriageRuleException invalid(String message) {
		return new InvalidTriageRuleException(Map.of("keywords", message));
	}

}
