package pe.edu.unmsm.servicios.auth.infrastructure.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.auth.application.ManagePasskeys;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.CreationOptions;
import pe.edu.unmsm.servicios.auth.infrastructure.web.PasskeyDtos.PasskeyResponse;
import pe.edu.unmsm.servicios.auth.infrastructure.web.PasskeyDtos.RegisterPasskeyRequest;
import pe.edu.unmsm.servicios.auth.infrastructure.web.PasskeyDtos.RegistrationOptionsRequest;
import pe.edu.unmsm.servicios.auth.infrastructure.web.PasskeyDtos.RenamePasskeyRequest;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

/** The signed-in user's passkeys; {@code /me/**} requires a token through the general security rule. */
@RestController
@RequestMapping("/api/v1/me/passkeys")
class PasskeyController {

	private final ManagePasskeys passkeys;

	PasskeyController(ManagePasskeys passkeys) {
		this.passkeys = passkeys;
	}

	@PostMapping("/registration/options")
	PasskeyOptions<CreationOptions> registrationOptions(@Valid @RequestBody RegistrationOptionsRequest request,
			@AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
		return passkeys.startRegistration(CurrentUser.from(jwt).id(), request.currentPassword(), http.getRemoteAddr());
	}

	@PostMapping
	ResponseEntity<PasskeyResponse> register(@Valid @RequestBody RegisterPasskeyRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		PasskeyResponse body = PasskeyResponse.from(passkeys.register(CurrentUser.from(jwt).id(), request.challengeId(),
				request.name(), request.credential().toString()));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping
	List<PasskeyResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return passkeys.list(CurrentUser.from(jwt).id()).stream().map(PasskeyResponse::from).toList();
	}

	@PatchMapping("/{id}")
	PasskeyResponse rename(@PathVariable long id, @Valid @RequestBody RenamePasskeyRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return PasskeyResponse.from(passkeys.rename(CurrentUser.from(jwt).id(), id, request.name()));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
		passkeys.delete(CurrentUser.from(jwt).id(), id);
		return ResponseEntity.noContent().build();
	}

}
