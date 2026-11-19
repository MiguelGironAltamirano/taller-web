package pe.edu.unmsm.servicios.auth.application;

import java.util.List;

/**
 * The options of a WebAuthn ceremony with the challenge that identifies it. {@code publicKey} follows the JSON forms
 * of the standard ({@code PublicKeyCredentialCreationOptionsJSON} / {@code PublicKeyCredentialRequestOptionsJSON}):
 * binary values are base64url without padding.
 */
public record PasskeyOptions<T>(String challengeId, T publicKey) {

	public record CreationOptions(String challenge, RelyingParty rp, UserAccount user,
			List<CredentialParameter> pubKeyCredParams, long timeout, List<CredentialDescriptor> excludeCredentials,
			AuthenticatorSelection authenticatorSelection, String attestation) {
	}

	public record RequestOptions(String challenge, String rpId, long timeout, String userVerification,
			List<CredentialDescriptor> allowCredentials) {
	}

	public record RelyingParty(String id, String name) {
	}

	public record UserAccount(String id, String name, String displayName) {
	}

	public record CredentialParameter(String type, long alg) {
	}

	public record CredentialDescriptor(String type, String id, List<String> transports) {
	}

	public record AuthenticatorSelection(String residentKey, boolean requireResidentKey, String userVerification) {
	}

}
