package pe.edu.unmsm.servicios.collaboration.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceRulesTest {

	@Test
	void validEvidenceUnderLimitsIsAccepted() {
		assertThatCode(() -> EvidenceRules.validate(0, 1024))
				.doesNotThrowAnyException();
		assertThatCode(() -> EvidenceRules.validate(4, 5 * 1024 * 1024))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectingSixthFileThrowsEvidenceLimitExceededException() {
		assertThatThrownBy(() -> EvidenceRules.validate(5, 1024))
				.isInstanceOf(EvidenceLimitExceededException.class)
				.hasMessageContaining("límite de 5 evidencias");
	}

	@Test
	void rejectingFileOver5MbThrowsEvidenceSizeLimitExceededException() {
		long over5Mb = (5 * 1024 * 1024) + 1;
		assertThatThrownBy(() -> EvidenceRules.validate(2, over5Mb))
				.isInstanceOf(EvidenceSizeLimitExceededException.class)
				.hasMessageContaining("5 MB");
	}

	@Test
	void rejectingEmptyFileThrowsEmptyFileException() {
		assertThatThrownBy(() -> EvidenceRules.validate(0, 0))
				.isInstanceOf(EmptyFileException.class)
				.hasMessageContaining("vacío");
	}

}
