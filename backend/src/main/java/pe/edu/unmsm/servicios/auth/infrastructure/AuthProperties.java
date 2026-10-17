package pe.edu.unmsm.servicios.auth.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.auth")
public record AuthProperties(@DefaultValue("true") boolean refreshCookieSecure) {
}
