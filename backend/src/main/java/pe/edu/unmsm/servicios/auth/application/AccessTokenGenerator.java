package pe.edu.unmsm.servicios.auth.application;

import pe.edu.unmsm.servicios.auth.domain.User;

public interface AccessTokenGenerator {

	record AccessToken(String value, long expiresInSeconds) {
	}

	AccessToken generate(User user);

}
