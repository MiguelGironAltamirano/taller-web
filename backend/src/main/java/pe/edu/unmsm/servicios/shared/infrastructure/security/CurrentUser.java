package pe.edu.unmsm.servicios.shared.infrastructure.security;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static AuthenticatedUser from(Jwt jwt) {
		List<String> roles = jwt.getClaimAsStringList("roles");
		return new AuthenticatedUser(Long.parseLong(jwt.getSubject()), jwt.getClaimAsString("email"),
				jwt.getClaimAsString("name"), roles.getFirst());
	}

}
