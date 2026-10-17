package pe.edu.unmsm.servicios.shared.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl, String issuer) {
}
