package pe.edu.unmsm.servicios.auth.application;

import pe.edu.unmsm.servicios.auth.application.AccessTokenGenerator.AccessToken;
import pe.edu.unmsm.servicios.auth.domain.User;

public record AuthSession(User user, AccessToken accessToken, String refreshToken) {
}
