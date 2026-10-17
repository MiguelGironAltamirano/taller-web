package pe.edu.unmsm.servicios.auth.infrastructure;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.auth.application.AccessTokenGenerator;
import pe.edu.unmsm.servicios.auth.application.Login;
import pe.edu.unmsm.servicios.auth.application.LoginAttemptLimiter;
import pe.edu.unmsm.servicios.auth.application.Logout;
import pe.edu.unmsm.servicios.auth.application.PasswordHasher;
import pe.edu.unmsm.servicios.auth.application.RefreshSession;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenCodec;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;
import pe.edu.unmsm.servicios.auth.application.RegisterUser;
import pe.edu.unmsm.servicios.auth.application.UserRepository;
import pe.edu.unmsm.servicios.shared.infrastructure.security.JwtProperties;

@Configuration
@EnableConfigurationProperties({ AuthProperties.class, LoginLimitProperties.class })
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
	LoginAttemptLimiter loginAttemptLimiter(Clock clock, LoginLimitProperties limit) {
		return new LoginAttemptLimiter(clock, limit.maxFailuresPerClient(), limit.maxFailuresPerEmail(), limit.window(),
				limit.maxTrackedKeys());
	}

	@Bean
	Login login(UserRepository users, RefreshTokenRepository refreshTokens, PasswordHasher hasher,
			RefreshTokenCodec codec, AccessTokenGenerator accessTokens, Clock clock, JwtProperties jwt,
			LoginAttemptLimiter limiter) {
		return new Login(users, refreshTokens, hasher, codec, accessTokens, clock, jwt.refreshTokenTtl(), limiter);
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
