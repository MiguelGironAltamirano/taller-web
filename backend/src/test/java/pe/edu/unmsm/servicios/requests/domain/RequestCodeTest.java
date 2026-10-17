package pe.edu.unmsm.servicios.requests.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RequestCodeTest {

	@Test
	void formatsYearAndSixDigitSequence() {
		assertThat(new RequestCode(2026, 1).value()).isEqualTo("SOL-2026-000001");
		assertThat(new RequestCode(2026, 123).value()).isEqualTo("SOL-2026-000123");
		assertThat(new RequestCode(2027, 999999).value()).isEqualTo("SOL-2027-999999");
	}

	@Test
	void rejectsSequenceBelowOne() {
		assertThatThrownBy(() -> new RequestCode(2026, 0)).isInstanceOf(IllegalArgumentException.class);
	}

}
