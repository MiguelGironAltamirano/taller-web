package pe.edu.unmsm.servicios.shared.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import pe.edu.unmsm.servicios.shared.application.SessionRegistry;
import pe.edu.unmsm.servicios.shared.application.SessionRegistry.SessionState;

/**
 * Accepts an access token only while its session ({@code sid}) is open, so a closed session stops working at once
 * instead of when the token expires (docs/api/contrato-v1.md «Una sola sesión por usuario»).
 */
class OpenSessionValidator implements OAuth2TokenValidator<Jwt> {

	/** Error code of a session closed by a newer login; the 401 then tells the client why. */
	static final String SESSION_REPLACED = "session_replaced";

	private final SessionRegistry sessions;

	OpenSessionValidator(SessionRegistry sessions) {
		this.sessions = sessions;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		String sessionId = jwt.getClaimAsString(AccessTokenIssuer.SID_CLAIM);
		long userId;
		try {
			userId = Long.parseLong(jwt.getSubject());
		}
		catch (NumberFormatException ex) {
			return closed("The token has no valid subject");
		}
		if (sessionId == null || sessionId.isBlank()) {
			return closed("The token is not bound to a session");
		}
		SessionState state = sessions.stateOf(userId, sessionId);
		return switch (state) {
			case OPEN -> OAuth2TokenValidatorResult.success();
			case REPLACED -> OAuth2TokenValidatorResult.failure(
					new OAuth2Error(SESSION_REPLACED, "The session was replaced by a newer login", null));
			case CLOSED -> closed("The session is closed");
		};
	}

	private static OAuth2TokenValidatorResult closed(String description) {
		return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
	}

}
