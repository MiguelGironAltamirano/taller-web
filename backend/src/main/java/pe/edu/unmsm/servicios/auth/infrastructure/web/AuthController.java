package pe.edu.unmsm.servicios.auth.infrastructure.web;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.auth.application.AuthSession;
import pe.edu.unmsm.servicios.auth.application.Login;
import pe.edu.unmsm.servicios.auth.application.Logout;
import pe.edu.unmsm.servicios.auth.application.PasskeyLogin;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.RequestOptions;
import pe.edu.unmsm.servicios.auth.application.RefreshSession;
import pe.edu.unmsm.servicios.auth.application.RegisterUser;
import pe.edu.unmsm.servicios.auth.domain.User;
import pe.edu.unmsm.servicios.auth.infrastructure.AuthProperties;
import pe.edu.unmsm.servicios.auth.infrastructure.web.AuthDtos.LoginRequest;
import pe.edu.unmsm.servicios.auth.infrastructure.web.AuthDtos.RegisterRequest;
import pe.edu.unmsm.servicios.auth.infrastructure.web.AuthDtos.TokenResponse;
import pe.edu.unmsm.servicios.auth.infrastructure.web.AuthDtos.UserResponse;
import pe.edu.unmsm.servicios.auth.infrastructure.web.PasskeyDtos.PasskeyLoginRequest;
import pe.edu.unmsm.servicios.shared.infrastructure.security.JwtProperties;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private static final String REFRESH_COOKIE = "refresh_token";
	private static final String COOKIE_PATH = "/api/v1/auth";

	private final RegisterUser registerUser;
	private final Login login;
	private final RefreshSession refreshSession;
	private final Logout logout;
	private final PasskeyLogin passkeyLogin;
	private final pe.edu.unmsm.servicios.auth.application.ChangePassword changePassword;
	private final AuthProperties authProperties;
	private final JwtProperties jwtProperties;

	AuthController(RegisterUser registerUser, Login login, RefreshSession refreshSession, Logout logout,
			PasskeyLogin passkeyLogin, pe.edu.unmsm.servicios.auth.application.ChangePassword changePassword,
			AuthProperties authProperties, JwtProperties jwtProperties) {
		this.registerUser = registerUser;
		this.login = login;
		this.refreshSession = refreshSession;
		this.logout = logout;
		this.passkeyLogin = passkeyLogin;
		this.changePassword = changePassword;
		this.authProperties = authProperties;
		this.jwtProperties = jwtProperties;
	}

	@org.springframework.web.bind.annotation.PutMapping("/password")
	ResponseEntity<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request,
			@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
		pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser user = pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser.from(jwt);
		changePassword.execute(user.id(), request.currentPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/register")
	ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = registerUser.execute(new RegisterUser.Command(request.fullName(), request.email(),
				request.password(), request.requesterType()));
		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
	}

	@PostMapping("/login")
	ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		return withSession(login.execute(request.email(), request.password(), http.getRemoteAddr()));
	}

	/** Login with fingerprint or passkey, step 1: the challenge (docs/api/passkeys-v1.md §4). */
	@PostMapping("/passkeys/login/options")
	PasskeyOptions<RequestOptions> passkeyLoginOptions(HttpServletRequest http) {
		return passkeyLogin.start(http.getRemoteAddr());
	}

	/** Step 2: the signed challenge; on success the same session as a password login. */
	@PostMapping("/passkeys/login")
	ResponseEntity<TokenResponse> passkeyLogin(@Valid @RequestBody PasskeyLoginRequest request,
			HttpServletRequest http) {
		return withSession(passkeyLogin.finish(request.challengeId(), request.credential().toString(),
				http.getRemoteAddr()));
	}

	@PostMapping("/refresh")
	ResponseEntity<TokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
		return withSession(refreshSession.execute(token));
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
		logout.execute(token);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
	}

	private ResponseEntity<TokenResponse> withSession(AuthSession session) {
		TokenResponse body = new TokenResponse(session.accessToken().value(), session.accessToken().expiresInSeconds(),
				UserResponse.from(session.user()));
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie(session.refreshToken(), jwtProperties.refreshTokenTtl()).toString())
				.body(body);
	}

	private ResponseCookie cookie(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
				.httpOnly(true)
				.secure(authProperties.refreshCookieSecure())
				.sameSite("Strict")
				.path(COOKIE_PATH)
				.maxAge(maxAge)
				.build();
	}

}
