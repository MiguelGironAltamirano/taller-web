package pe.edu.unmsm.servicios.auth.application;

import pe.edu.unmsm.servicios.auth.domain.User;

public interface AccessTokenGenerator {

	record AccessToken(String value, long expiresInSeconds) {
	}

	/** An access token bound to {@code sessionId}: it stops working as soon as that session closes. */
	AccessToken generate(User user, String sessionId);

}
