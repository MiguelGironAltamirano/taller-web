package pe.edu.unmsm.servicios.triage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class TriageKeywordsTest {

	@Test
	void keywordsAreTrimmedLowercasedAndDeduplicatedKeepingTheirOrder() {
		assertThat(TriageKeywords.clean(List.of("  Humo ", "Aula   Virtual", "humo", "HÚMO", "chispa")))
				.containsExactly("humo", "aula virtual", "chispa");
	}

	@Test
	void atLeastOneKeywordIsRequired() {
		assertInvalid(List.of());
		assertInvalid(null);
	}

	@Test
	void atMostTwentyKeywordsAreAllowed() {
		List<String> keywords = new ArrayList<>();
		for (int i = 0; i < 21; i++) {
			keywords.add("palabra" + i);
		}
		assertInvalid(keywords);
		assertThat(TriageKeywords.clean(keywords.subList(0, 20))).hasSize(20);
	}

	@Test
	void eachKeywordHasBetweenTwoAndFiftyCharacters() {
		assertInvalid(List.of("a"));
		assertInvalid(List.of("   "));
		assertInvalid(List.of(String.join("", Collections.nCopies(51, "x"))));
		assertInvalid(Collections.singletonList(null));
		assertThat(TriageKeywords.clean(List.of("ab", String.join("", Collections.nCopies(50, "x"))))).hasSize(2);
	}

	private static void assertInvalid(List<String> keywords) {
		assertThatThrownBy(() -> TriageKeywords.clean(keywords)).isInstanceOfSatisfying(
				InvalidTriageRuleException.class, ex -> assertThat(ex.errors()).containsKey("keywords"));
	}

}
