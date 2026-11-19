package pe.edu.unmsm.servicios.auth.infrastructure;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Relying party of the passkey ceremonies, per environment (docs/api/passkeys-v1.md §3). */
@ConfigurationProperties("app.auth.webauthn")
public record WebAuthnProperties(
		@DefaultValue("localhost") String rpId,
		@DefaultValue("Servicios Universitarios UNMSM") String rpName,
		@DefaultValue("http://localhost:8088") List<String> allowedOrigins,
		@DefaultValue("5m") Duration challengeTtl,
		@DefaultValue("10") int maxPasskeysPerUser,
		@DefaultValue("30") int maxChallengesPerClient) {

	public WebAuthnProperties {
		allowedOrigins = allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList();
		if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
			throw new IllegalStateException("app.auth.webauthn.allowed-origins must list explicit origins");
		}
	}

}
