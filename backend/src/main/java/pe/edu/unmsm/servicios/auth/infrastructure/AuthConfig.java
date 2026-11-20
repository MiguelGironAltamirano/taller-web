package pe.edu.unmsm.servicios.auth.infrastructure;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.auth.application.AccessTokenGenerator;
import pe.edu.unmsm.servicios.auth.application.Login;
import pe.edu.unmsm.servicios.auth.application.LoginAttemptLimiter;
import pe.edu.unmsm.servicios.auth.application.Logout;
import pe.edu.unmsm.servicios.auth.application.ManagePasskeys;
import pe.edu.unmsm.servicios.auth.application.PasskeyLogin;
import pe.edu.unmsm.servicios.auth.application.PasskeyRepository;
import pe.edu.unmsm.servicios.auth.application.PasskeySettings;
import pe.edu.unmsm.servicios.auth.application.PasskeyVerifier;
import pe.edu.unmsm.servicios.auth.application.PasswordHasher;
import pe.edu.unmsm.servicios.auth.application.RefreshSession;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenCodec;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;
import pe.edu.unmsm.servicios.auth.application.RegisterUser;
import pe.edu.unmsm.servicios.auth.application.SessionIssuer;
import pe.edu.unmsm.servicios.auth.application.UserRepository;
import pe.edu.unmsm.servicios.auth.application.WebAuthnChallengeRepository;
import pe.edu.unmsm.servicios.auth.application.WebAuthnUserHandles;
import pe.edu.unmsm.servicios.shared.infrastructure.security.JwtProperties;

@Configuration
@EnableConfigurationProperties({ AuthProperties.class, LoginLimitProperties.class, WebAuthnProperties.class })
class AuthConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	RegisterUser registerUser(UserRepository users, PasswordHasher hasher) {
		return new RegisterUser(users, hasher);
	}

	@Bean
	pe.edu.unmsm.servicios.auth.application.ChangePassword changePassword(UserRepository users,
			RefreshTokenRepository refreshTokens, PasswordHasher hasher) {
		return new pe.edu.unmsm.servicios.auth.application.ChangePassword(users, refreshTokens, hasher);
	}

	@Bean
	LoginAttemptLimiter loginAttemptLimiter(Clock clock, LoginLimitProperties limit) {
		return new LoginAttemptLimiter(clock, limit.maxFailuresPerClient(), limit.maxFailuresPerEmail(), limit.window(),
				limit.maxTrackedKeys());
	}

	@Bean
	SessionIssuer sessionIssuer(RefreshTokenRepository refreshTokens, RefreshTokenCodec codec,
			AccessTokenGenerator accessTokens, Clock clock, JwtProperties jwt) {
		return new SessionIssuer(refreshTokens, codec, accessTokens, clock, jwt.refreshTokenTtl());
	}

	@Bean
	Login login(UserRepository users, PasswordHasher hasher, SessionIssuer sessions, LoginAttemptLimiter limiter) {
		return new Login(users, hasher, sessions, limiter);
	}

	@Bean
	PasskeySettings passkeySettings(WebAuthnProperties webAuthn) {
		return new PasskeySettings(webAuthn.rpId(), webAuthn.rpName(), webAuthn.challengeTtl(),
				webAuthn.maxPasskeysPerUser());
	}

	@Bean
	PasskeyVerifier passkeyVerifier(WebAuthnProperties webAuthn) {
		return new WebAuthn4jPasskeyVerifier(webAuthn);
	}

	/**
	 * Counts every passkey login challenge requested per client (never reset), so challenges cannot be generated
	 * without bound. Kept apart from the failure limiter: each login legitimately asks for one.
	 */
	@Bean
	PasskeyLogin passkeyLogin(PasskeyRepository passkeys, WebAuthnChallengeRepository challenges,
			WebAuthnUserHandles handles, UserRepository users, PasskeyVerifier verifier, SessionIssuer sessions,
			LoginAttemptLimiter loginAttemptLimiter, PasskeySettings settings, WebAuthnProperties webAuthn,
			LoginLimitProperties limit, Clock clock) {
		LoginAttemptLimiter challengeRequests = new LoginAttemptLimiter(clock, webAuthn.maxChallengesPerClient(),
				webAuthn.maxChallengesPerClient(), limit.window(), limit.maxTrackedKeys());
		return new PasskeyLogin(passkeys, challenges, handles, users, verifier, sessions, loginAttemptLimiter,
				challengeRequests, settings, clock);
	}

	@Bean
	ManagePasskeys managePasskeys(PasskeyRepository passkeys, WebAuthnChallengeRepository challenges,
			WebAuthnUserHandles handles, UserRepository users, PasswordHasher hasher, PasskeyVerifier verifier,
			LoginAttemptLimiter loginAttemptLimiter, PasskeySettings settings, Clock clock) {
		return new ManagePasskeys(passkeys, challenges, handles, users, hasher, verifier, loginAttemptLimiter,
				settings, clock);
	}

	@Bean
	RefreshSession refreshSession(UserRepository users, RefreshTokenRepository refreshTokens, RefreshTokenCodec codec,
			AccessTokenGenerator accessTokens, Clock clock, JwtProperties jwt) {
		return new RefreshSession(users, refreshTokens, codec, accessTokens, clock, jwt.refreshTokenTtl());
	}

	@Bean
	Logout logout(RefreshTokenRepository refreshTokens, RefreshTokenCodec codec, Clock clock) {
		return new Logout(refreshTokens, codec, clock);
	}

}
