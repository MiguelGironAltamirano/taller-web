package pe.edu.unmsm.servicios.auth.infrastructure.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
class MeController {

	@GetMapping("/api/v1/me")
	AuthenticatedUserResponse me(@AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		return new AuthenticatedUserResponse(user.id(), user.email(), user.fullName(), user.role());
	}

	record AuthenticatedUserResponse(long id, String email, String fullName, String role) {
	}

}
