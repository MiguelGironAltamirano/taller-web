package pe.edu.unmsm.servicios.auth.infrastructure;

import java.time.Clock;

import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;
import pe.edu.unmsm.servicios.shared.application.SessionRegistry;

/** A session is open while one of its refresh tokens is neither revoked nor expired. */
@Component
class RefreshTokenSessionRegistry implements SessionRegistry {

	private final RefreshTokenRepository refreshTokens;
	private final Clock clock;

	RefreshTokenSessionRegistry(RefreshTokenRepository refreshTokens, Clock clock) {
		this.refreshTokens = refreshTokens;
		this.clock = clock;
	}

	@Override
	public SessionState stateOf(long userId, String sessionId) {
		return refreshTokens.stateOf(userId, sessionId, clock.instant());
	}

}
