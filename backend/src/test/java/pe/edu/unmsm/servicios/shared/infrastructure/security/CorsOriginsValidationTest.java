package pe.edu.unmsm.servicios.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class CorsOriginsValidationTest {

	private final SecurityConfig config = new SecurityConfig();

	@Test
	void wildcardOriginsAreRejectedBecauseCredentialsAreAllowed() {
		assertThatThrownBy(() -> config.corsConfigurationSource(List.of("*")))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("app.cors.allowed-origins");
		assertThatThrownBy(() -> config.corsConfigurationSource(List.of("https://ok.pe", "https://*.example.pe")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void explicitOriginsAreAccepted() {
		var source = config.corsConfigurationSource(List.of("https://app.example.pe"));

		assertThat(source).isNotNull();
		CorsConfiguration cors = ((org.springframework.web.cors.UrlBasedCorsConfigurationSource) source)
				.getCorsConfigurations().get("/**");
		assertThat(cors.getAllowedOrigins()).containsExactly("https://app.example.pe");
	}

}
