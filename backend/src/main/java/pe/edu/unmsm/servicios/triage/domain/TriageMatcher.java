package pe.edu.unmsm.servicios.triage.domain;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Keyword matching of spec §5.1. A keyword matches when it appears in the description as a whole word or
 * phrase, ignoring case and accents. Each keyword counts once per rule.
 */
public final class TriageMatcher {

	/** Letters and digits glue words together; anything else (spaces, punctuation) is a boundary. */
	private static final String WORD_CHAR = "[\\p{L}\\p{N}]";

	private TriageMatcher() {
	}

	public record RuleMatch(TriageRule rule, int hits) {
	}

	/** Active rules with at least one match, best first: more matches, then the oldest (lowest id). */
	public static List<RuleMatch> rank(String description, List<TriageRule> rules) {
		String text = TriageText.normalize(description);
		return rules.stream()
				.filter(TriageRule::active)
				.map(rule -> new RuleMatch(rule, hits(text, rule.keywords())))
				.filter(match -> match.hits() > 0)
				.sorted(Comparator.comparingInt(RuleMatch::hits).reversed()
						.thenComparingLong(match -> match.rule().id()))
				.toList();
	}

	private static int hits(String text, List<String> keywords) {
		return (int) keywords.stream()
				.map(TriageText::normalize)
				.distinct()
				.filter(keyword -> !keyword.isEmpty() && appears(text, keyword))
				.count();
	}

	private static boolean appears(String text, String keyword) {
		return Pattern.compile("(?<!" + WORD_CHAR + ")" + Pattern.quote(keyword) + "(?!" + WORD_CHAR + ")")
				.matcher(text).find();
	}

}
