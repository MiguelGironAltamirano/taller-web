package pe.edu.unmsm.servicios.shared.infrastructure.security;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static AuthenticatedUser from(Jwt jwt) {
		List<String> roles = jwt.getClaimAsStringList("roles");
		Object area = jwt.getClaim("area_id");
		Long areaId = area instanceof Number number ? number.longValue() : null;
		return new AuthenticatedUser(Long.parseLong(jwt.getSubject()), jwt.getClaimAsString("email"),
				jwt.getClaimAsString("name"), roles.getFirst(), areaId);
	}

}
