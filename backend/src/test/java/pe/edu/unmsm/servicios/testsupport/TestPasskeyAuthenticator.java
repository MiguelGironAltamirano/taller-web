package pe.edu.unmsm.servicios.testsupport;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import com.webauthn4j.converter.AttestationObjectConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;

/**
 * A software passkey for tests: one ES256 key pair and credential, producing real WebAuthn responses (the JSON a
 * browser sends) that the backend verifies cryptographically. It stands in for the fingerprint sensor: it always
 * reports the user as present and verified unless told otherwise.
 */
public final class TestPasskeyAuthenticator {

	private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();
	private static final byte UP = 0x01;
	private static final byte UV = 0x04;
	private static final byte AT = 0x40;

	private final KeyPair keys;
	private final byte[] credentialId;
	private final String origin;
	private final String rpId;
	private byte[] userHandle;
	private long counter;

	public TestPasskeyAuthenticator(String origin, String rpId) {
		this(newKeyPair(), random(32), origin, rpId);
	}

	private TestPasskeyAuthenticator(KeyPair keys, byte[] credentialId, String origin, String rpId) {
		this.keys = keys;
		this.credentialId = credentialId;
		this.origin = origin;
		this.rpId = rpId;
	}

	/** The same credential used from another page, as a phishing site would try. */
	public TestPasskeyAuthenticator from(String otherOrigin) {
		TestPasskeyAuthenticator copy = new TestPasskeyAuthenticator(keys, credentialId, otherOrigin, rpId);
		copy.userHandle = userHandle;
		copy.counter = counter;
		return copy;
	}

	/** A passkey the server never registered, with a user handle of its own. */
	public TestPasskeyAuthenticator registeredLess() {
		userHandle = random(64);
		return this;
	}

	private static KeyPair newKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
			generator.initialize(new ECGenParameterSpec("secp256r1"));
			return generator.generateKeyPair();
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[] random(int length) {
		byte[] bytes = new byte[length];
		new SecureRandom().nextBytes(bytes);
		return bytes;
	}

	public String credentialId() {
		return B64.encodeToString(credentialId);
	}

	/** {@code RegistrationResponseJSON} for a challenge and user handle given by the server (both base64url). */
	public String register(String challenge, String userHandle) {
		this.userHandle = B64_DECODER.decode(userHandle);
		AttestedCredentialData attested = new AttestedCredentialData(AAGUID.ZERO, credentialId,
				EC2COSEKey.create((ECPublicKey) keys.getPublic(), COSEAlgorithmIdentifier.ES256));
		AuthenticatorData<?> authenticatorData = new AuthenticatorData<>(sha256(rpId.getBytes(StandardCharsets.UTF_8)),
				(byte) (UP | UV | AT), counter, attested);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		AttestationObject attestation = new AttestationObject((AuthenticatorData) authenticatorData,
				new NoneAttestationStatement());
		byte[] attestationObject = new AttestationObjectConverter(new ObjectConverter()).convertToBytes(attestation);
		String clientData = clientData("webauthn.create", challenge);
		return """
				{"id":"%1$s","rawId":"%1$s","type":"public-key","authenticatorAttachment":"platform",
				 "response":{"clientDataJSON":"%2$s","attestationObject":"%3$s","transports":["internal"]},
				 "clientExtensionResults":{}}"""
				.formatted(credentialId(), b64(clientData), B64.encodeToString(attestationObject));
	}

	/** {@code AuthenticationResponseJSON} for a login challenge, signed with the private key. */
	public String login(String challenge) {
		return login(challenge, true, ++counter);
	}

	/** Same, choosing the user-verified flag and the signature counter (to simulate failures). */
	public String login(String challenge, boolean userVerified, long signCount) {
		ByteBuffer authenticatorData = ByteBuffer.allocate(37);
		authenticatorData.put(sha256(rpId.getBytes(StandardCharsets.UTF_8)));
		authenticatorData.put((byte) (userVerified ? UP | UV : UP));
		authenticatorData.putInt((int) signCount);
		byte[] authData = authenticatorData.array();
		String clientData = clientData("webauthn.get", challenge);
		byte[] signature;
		try {
			Signature signer = Signature.getInstance("SHA256withECDSA");
			signer.initSign(keys.getPrivate());
			ByteArrayOutputStream signed = new ByteArrayOutputStream();
			signed.writeBytes(authData);
			signed.writeBytes(sha256(clientData.getBytes(StandardCharsets.UTF_8)));
			signer.update(signed.toByteArray());
			signature = signer.sign();
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
		return """
				{"id":"%1$s","rawId":"%1$s","type":"public-key","authenticatorAttachment":"platform",
				 "response":{"clientDataJSON":"%2$s","authenticatorData":"%3$s","signature":"%4$s","userHandle":"%5$s"},
				 "clientExtensionResults":{}}"""
				.formatted(credentialId(), b64(clientData), B64.encodeToString(authData),
						B64.encodeToString(signature), B64.encodeToString(userHandle));
	}

	private String clientData(String type, String challenge) {
		return "{\"type\":\"%s\",\"challenge\":\"%s\",\"origin\":\"%s\",\"crossOrigin\":false}".formatted(type,
				challenge, origin);
	}

	private static String b64(String text) {
		return B64.encodeToString(text.getBytes(StandardCharsets.UTF_8));
	}

	private static byte[] sha256(byte[] data) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

}
