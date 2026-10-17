package pe.edu.unmsm.servicios.admin.application;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.admin.domain.ManagedUser;
import pe.edu.unmsm.servicios.admin.domain.ManagedUserNotFoundException;
import pe.edu.unmsm.servicios.admin.domain.SelfManagementException;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;

public class SetUserActive {

	private final AdminUserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final Clock clock;

	public SetUserActive(AdminUserRepository users, RefreshTokenRepository refreshTokens, Clock clock) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
	}

	/** Deactivating ends the user's sessions: their refresh tokens are revoked at once. */
	@Transactional
	public ManagedUser execute(long actorId, long userId, boolean active) {
		users.findById(userId).orElseThrow(() -> new ManagedUserNotFoundException(userId));
		if (actorId == userId && !active) {
			throw new SelfManagementException("No puedes desactivar tu propia cuenta");
		}
		ManagedUser updated = users.setActive(userId, active);
		if (!active) {
			refreshTokens.revokeAllForUser(userId, clock.instant());
		}
		return updated;
	}

}
