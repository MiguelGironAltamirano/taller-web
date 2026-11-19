package pe.edu.unmsm.servicios.auth.application;

import java.time.Duration;

/** Relying party and limits of the passkey ceremonies (see docs/api/passkeys-v1.md §3). */
public record PasskeySettings(String rpId, String rpName, Duration challengeTtl, int maxPasskeysPerUser) {

	/** How long the browser waits for the user, in milliseconds. */
	public static final long CEREMONY_TIMEOUT_MILLIS = 60_000;

}
