package pe.edu.unmsm.servicios.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

	private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

	/** Same policy that registration enforces, so the user can keep using the account normally. */
	@RepeatedTest(50)
	void passwordsHaveTwelveAlphanumericCharactersWithALetterAndADigit() {
		String password = generator.generate();

		assertThat(password).hasSize(12).matches("[A-Za-z0-9]+").matches(".*[A-Za-z].*").matches(".*\\d.*");
	}

	@Test
	void passwordsAreNotRepeated() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			seen.add(generator.generate());
		}

		assertThat(seen).hasSize(200);
	}

}
