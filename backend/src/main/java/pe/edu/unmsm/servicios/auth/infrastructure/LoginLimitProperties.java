package pe.edu.unmsm.servicios.auth.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.auth.login-limit")
public record LoginLimitProperties(
		@DefaultValue("5") int maxFailuresPerClient,
		@DefaultValue("20") int maxFailuresPerEmail,
		@DefaultValue("15m") Duration window,
		@DefaultValue("10000") int maxTrackedKeys) {
}
