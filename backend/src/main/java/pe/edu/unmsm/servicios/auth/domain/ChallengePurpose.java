package pe.edu.unmsm.servicios.auth.domain;

/** The WebAuthn ceremony a challenge was issued for; a challenge only serves its own ceremony. */
public enum ChallengePurpose {
	LOGIN, REGISTRATION
}
