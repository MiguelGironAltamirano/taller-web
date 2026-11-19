package pe.edu.unmsm.servicios.auth.application;

import pe.edu.unmsm.servicios.auth.domain.InvalidPasskeyException;
import pe.edu.unmsm.servicios.auth.domain.Passkey;

/**
 * Cryptographic checks of the WebAuthn ceremonies against the configured relying party (id and allowed origins).
 * Every method throws {@link InvalidPasskeyException} when the response cannot be parsed or does not verify.
 */
public interface PasskeyVerifier {

	/** Verifies a registration response ({@code RegistrationResponseJSON}) issued for {@code challenge}. */
	VerifiedRegistration verifyRegistration(String responseJson, byte[] challenge);

	/** Reads the credential id and user handle of an authentication response, before it is verified. */
	AssertionClaim readAssertion(String responseJson);

	/** Verifies an authentication response ({@code AuthenticationResponseJSON}) with the stored passkey. */
	VerifiedAssertion verifyAssertion(String responseJson, byte[] challenge, Passkey passkey);

	record VerifiedRegistration(byte[] credentialId, byte[] publicKey, long signCount, String transports,
			boolean backupEligible, boolean backedUp) {
	}

	record AssertionClaim(byte[] credentialId, byte[] userHandle) {
	}

	record VerifiedAssertion(long signCount, boolean backedUp) {
	}

}
