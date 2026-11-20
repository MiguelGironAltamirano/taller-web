package pe.edu.unmsm.servicios.shared.application;

/**
 * Tells whether the session named by an access token is still open. Checked on every authenticated request, so
 * closing a session (a newer login, logout, a password change, a deactivated account) takes effect at once.
 */
public interface SessionRegistry {

	enum SessionState {
		/** Open: the request may proceed. */
		OPEN,
		/** Closed because the same user opened another session, which is still open. */
		REPLACED,
		/** Closed for any other reason, or never existed. */
		CLOSED
	}

	SessionState stateOf(long userId, String sessionId);

}
