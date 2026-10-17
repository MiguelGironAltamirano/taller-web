package pe.edu.unmsm.servicios.shared.infrastructure.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

@Component
public class AccessTokenIssuer {

	public record IssuedToken(String value, long expiresInSeconds) {
	}

	private final JwtEncoder encoder;
	private final JwtProperties properties;

	AccessTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	public IssuedToken issue(AuthenticatedUser user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(Long.toString(user.id()))
				.issuedAt(now)
				.expiresAt(now.plus(properties.accessTokenTtl()))
				.claim("email", user.email())
				.claim("name", user.fullName())
				.claim("roles", List.of(user.role()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(value, properties.accessTokenTtl().toSeconds());
	}

}
