package pe.edu.unmsm.servicios.shared.infrastructure.security;

import java.nio.charset.StandardCharsets;

import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

	private static final String[] PUBLIC_PATHS = { "/api/v1/auth/**", "/api/v1/system/**", "/v3/api-docs/**",
			"/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**", "/error" };

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, ProblemDetailsSecurityHandler problems) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.headers(headers -> headers
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
						.frameOptions(frame -> frame.deny()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(problems)
						.accessDeniedHandler(problems))
				.exceptionHandling(errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems))
				.build();
	}

	/** Cross-origin access is denied unless {@code app.cors.allowed-origins} lists the allowed origins. */
	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		List<String> origins = allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList();
		if (origins.stream().anyMatch(origin -> origin.contains("*"))) {
			throw new IllegalStateException("app.cors.allowed-origins must list explicit origins; wildcards are "
					+ "not allowed because credentials are enabled");
		}
		if (!origins.isEmpty()) {
			CorsConfiguration cors = new CorsConfiguration();
			cors.setAllowedOrigins(origins);
			cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
			cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
			cors.setAllowCredentials(true);
			source.registerCorsConfiguration("/**", cors);
		}
		return source;
	}

	@Bean
	SecretKey jwtSecretKey(JwtProperties properties) {
		byte[] bytes = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("roles");
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

}
