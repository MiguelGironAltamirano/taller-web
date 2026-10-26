package pe.edu.unmsm.servicios.triage.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TriageMatcherTest {

	private static TriageRule rule(long id, boolean active, String... keywords) {
		return new TriageRule(id, List.of(keywords), id * 10, id * 100, active);
	}

	private static List<Long> ranked(String description, TriageRule... rules) {
		return TriageMatcher.rank(description, List.of(rules)).stream().map(match -> match.rule().id()).toList();
	}

	@Test
	void theRuleWithMoreMatchingKeywordsComesFirst() {
		TriageRule electrical = rule(1, true, "luz", "tablero");
		TriageRule fire = rule(2, true, "chispa", "humo", "tablero");

		List<TriageMatcher.RuleMatch> matches = TriageMatcher.rank("Sale humo y una chispa del tablero",
				List.of(electrical, fire));

		assertThat(matches).extracting(match -> match.rule().id()).containsExactly(2L, 1L);
		assertThat(matches).extracting(TriageMatcher.RuleMatch::hits).containsExactly(3, 1);
	}

	@Test
	void aTieIsWonByTheOldestRule() {
		assertThat(ranked("No hay internet ni wifi", rule(7, true, "wifi"), rule(3, true, "internet")))
				.containsExactly(3L, 7L);
	}

	@Test
	void caseAndAccentsAreIgnoredOnBothSides() {
		assertThat(ranked("SE CORTÓ LA CONEXION en el baño", rule(1, true, "conexión"), rule(2, true, "BANO")))
				.containsExactly(1L, 2L);
	}

	@Test
	void keywordsMatchOnlyWholeWords() {
		assertThat(ranked("Las luces del pasillo parpadean", rule(1, true, "luz", "luce"))).isEmpty();
		assertThat(ranked("El foco, la luz; todo falla", rule(1, true, "luz"))).containsExactly(1L);
	}

	@Test
	void phrasesMatchAsAWholeIgnoringExtraSpaces() {
		assertThat(ranked("No puedo entrar al aula   virtual desde ayer", rule(1, true, "aula virtual")))
				.containsExactly(1L);
		assertThat(ranked("El aula de la facultad es virtual", rule(1, true, "aula virtual"))).isEmpty();
	}

	@Test
	void eachKeywordCountsOnceEvenIfRepeatedInTheText() {
		assertThat(TriageMatcher.rank("humo humo humo", List.of(rule(1, true, "humo"))))
				.extracting(TriageMatcher.RuleMatch::hits).containsExactly(1);
	}

	@Test
	void inactiveRulesAndRulesWithoutMatchesAreLeftOut() {
		assertThat(ranked("El proyector no enciende", rule(1, false, "proyector"), rule(2, true, "wifi"))).isEmpty();
	}

	@Test
	void normalizationLowercasesStripsAccentsAndCollapsesSpaces() {
		assertThat(TriageText.normalize("  Ñandú   ELÉCTRICO\tbaño ")).isEqualTo("nandu electrico bano");
	}

}
