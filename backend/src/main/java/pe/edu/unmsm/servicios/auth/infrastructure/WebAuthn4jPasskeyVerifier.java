package pe.edu.unmsm.servicios.auth.infrastructure;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.MaliciousCounterValueException;

import pe.edu.unmsm.servicios.auth.application.PasskeyVerifier;
import pe.edu.unmsm.servicios.auth.domain.InvalidPasskeyException;
import pe.edu.unmsm.servicios.auth.domain.Passkey;

/**
 * {@link PasskeyVerifier} with webauthn4j. Both ceremonies require user verification (the fingerprint, face or PIN
 * was checked on the device) and user presence. Attestation is not verified ({@code attestation: "none"}), and the
 * signature counter must grow unless the authenticator always reports 0 (synced passkeys).
 */
class WebAuthn4jPasskeyVerifier implements PasskeyVerifier {

	private static final Logger LOG = LoggerFactory.getLogger(WebAuthn4jPasskeyVerifier.class);
	private static final List<PublicKeyCredentialParameters> ALGORITHMS = List.of(
			new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
			new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256));

	private final ObjectConverter converter = new ObjectConverter();
	private final WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager(converter);
	private final AttestedCredentialDataConverter credentialData = new AttestedCredentialDataConverter(converter);
	private final Set<Origin> origins;
	private final String rpId;

	WebAuthn4jPasskeyVerifier(WebAuthnProperties properties) {
		this.origins = properties.allowedOrigins().stream().map(Origin::new).collect(Collectors.toSet());
		this.rpId = properties.rpId();
	}

	@Override
	public VerifiedRegistration verifyRegistration(String responseJson, byte[] challenge) {
		try {
			RegistrationData data = manager.parseRegistrationResponseJSON(responseJson);
			manager.verify(data, new RegistrationParameters(server(challenge), ALGORITHMS, true, true));
			AuthenticatorData<?> authenticatorData = data.getAttestationObject().getAuthenticatorData();
			AttestedCredentialData attested = authenticatorData.getAttestedCredentialData();
			String transports = data.getTransports() == null ? null
					: data.getTransports().stream().map(AuthenticatorTransport::getValue).sorted()
							.collect(Collectors.joining(","));
			return new VerifiedRegistration(attested.getCredentialId(), credentialData.convert(attested),
					authenticatorData.getSignCount(), transports, authenticatorData.isFlagBE(),
					authenticatorData.isFlagBS());
		}
		catch (InvalidPasskeyException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw invalid("registration", e);
		}
	}

	@Override
	public AssertionClaim readAssertion(String responseJson) {
		try {
			AuthenticationData data = manager.parseAuthenticationResponseJSON(responseJson);
			return new AssertionClaim(data.getCredentialId(), data.getUserHandle());
		}
		catch (RuntimeException e) {
			throw invalid("authentication", e);
		}
	}

	@Override
	public VerifiedAssertion verifyAssertion(String responseJson, byte[] challenge, Passkey passkey) {
		try {
			AuthenticationData data = manager.parseAuthenticationResponseJSON(responseJson);
			if (!Arrays.equals(data.getCredentialId(), passkey.credentialId())) {
				throw new InvalidPasskeyException("The response is for another credential");
			}
			CredentialRecordImpl record = new CredentialRecordImpl(new NoneAttestationStatement(), true,
					passkey.backupEligible(), passkey.backedUp(), passkey.signCount(),
					credentialData.convert(passkey.publicKey()), null, null, null, null);
			manager.verify(data, new AuthenticationParameters(server(challenge), record, null, true, true));
			AuthenticatorData<?> authenticatorData = data.getAuthenticatorData();
			return new VerifiedAssertion(authenticatorData.getSignCount(), authenticatorData.isFlagBS());
		}
		catch (MaliciousCounterValueException e) {
			LOG.warn("Passkey {} rejected: its signature counter did not grow (possible cloned authenticator)",
					passkey.id());
			throw new InvalidPasskeyException("Signature counter did not grow", e);
		}
		catch (InvalidPasskeyException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw invalid("authentication", e);
		}
	}

	/**
	 * The response comes from the browser and is untrusted: any parsing or verification error (webauthn4j reports
	 * malformed input with several runtime exceptions) means an invalid passkey, never a server error.
	 */
	private static InvalidPasskeyException invalid(String ceremony, RuntimeException cause) {
		return new InvalidPasskeyException("Invalid " + ceremony + " response: " + cause.getClass().getSimpleName(),
				cause);
	}

	private ServerProperty server(byte[] challenge) {
		return new ServerProperty(origins, rpId, new DefaultChallenge(challenge));
	}

}
