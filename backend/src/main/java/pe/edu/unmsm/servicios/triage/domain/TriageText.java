package pe.edu.unmsm.servicios.triage.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Text normalization shared by descriptions and keywords: lowercase, no accents, single spaces. */
public final class TriageText {

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");
	private static final Pattern SPACES = Pattern.compile("\\s+");

	private TriageText() {
	}

	public static String normalize(String text) {
		String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		return SPACES.matcher(MARKS.matcher(decomposed).replaceAll("")).replaceAll(" ").trim();
	}

	/** Trims and collapses inner whitespace, keeping case and accents. */
	static String tidy(String text) {
		return SPACES.matcher(text.trim()).replaceAll(" ");
	}

}
