package pe.edu.unmsm.servicios.auth.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.RequestOptions;
import pe.edu.unmsm.servicios.auth.application.PasskeyVerifier.AssertionClaim;
import pe.edu.unmsm.servicios.auth.application.PasskeyVerifier.VerifiedAssertion;
import pe.edu.unmsm.servicios.auth.domain.ChallengePurpose;
import pe.edu.unmsm.servicios.auth.domain.InvalidPasskeyException;
import pe.edu.unmsm.servicios.auth.domain.Passkey;
import pe.edu.unmsm.servicios.auth.domain.PasskeyLoginFailedException;
import pe.edu.unmsm.servicios.auth.domain.User;

/**
 * Login with a discoverable passkey: the user does not type the email, the device offers the passkeys saved for the
 * site. Every failure is the same {@link PasskeyLoginFailedException}, so a caller cannot tell which check failed.
 *
 * <p>Not transactional on purpose: the challenge is consumed even when the login then fails, so it never serves twice.
 */
public class PasskeyLogin {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

	private final PasskeyRepository passkeys;
	private final WebAuthnChallengeRepository challenges;
	private final WebAuthnUserHandles handles;
	private final UserRepository users;
	private final PasskeyVerifier verifier;
	private final SessionIssuer sessions;
	private final LoginAttemptLimiter failures;
	private final LoginAttemptLimiter challengeRequests;
	private final PasskeySettings settings;
	private final Clock clock;

	public PasskeyLogin(PasskeyRepository passkeys, WebAuthnChallengeRepository challenges, WebAuthnUserHandles handles,
			UserRepository users, PasskeyVerifier verifier, SessionIssuer sessions, LoginAttemptLimiter failures,
			LoginAttemptLimiter challengeRequests, PasskeySettings settings, Clock clock) {
		this.passkeys = passkeys;
		this.challenges = challenges;
		this.handles = handles;
		this.users = users;
		this.verifier = verifier;
		this.sessions = sessions;
		this.failures = failures;
		this.challengeRequests = challengeRequests;
		this.settings = settings;
		this.clock = clock;
	}

	/** Issues a login challenge; limited per client so challenges cannot be generated without bound. */
	public PasskeyOptions<RequestOptions> start(String clientIp) {
		challengeRequests.acquire(clientKey(clientIp), clientIp);
		Instant now = clock.instant();
		challenges.deleteExpired(now);
		byte[] challenge = new byte[32];
		RANDOM.nextBytes(challenge);
		String id = UUID.randomUUID().toString();
		challenges.save(id, ChallengePurpose.LOGIN, null, challenge, now.plus(settings.challengeTtl()));
		return new PasskeyOptions<>(id, new RequestOptions(BASE64URL.encodeToString(challenge), settings.rpId(),
				PasskeySettings.CEREMONY_TIMEOUT_MILLIS, "required", List.of()));
	}

	/** The checks of docs/api/passkeys-v1.md §4, in order; on success, the same session as a password login. */
	public AuthSession finish(String challengeId, String responseJson, String clientIp) {
		String key = clientKey(clientIp);
		failures.acquire(key, clientIp);
		Instant now = clock.instant();
		byte[] challenge = challenges.consume(challengeId, ChallengePurpose.LOGIN, null, now)
				.orElseThrow(PasskeyLoginFailedException::new);
		try {
			AssertionClaim claim = verifier.readAssertion(responseJson);
			Passkey passkey = passkeys.findByCredentialId(claim.credentialId())
					.orElseThrow(PasskeyLoginFailedException::new);
			byte[] handle = handles.find(passkey.userId()).orElseThrow(PasskeyLoginFailedException::new);
			if (claim.userHandle() == null || !Arrays.equals(handle, claim.userHandle())) {
				throw new PasskeyLoginFailedException();
			}
			VerifiedAssertion verified = verifier.verifyAssertion(responseJson, challenge, passkey);
			User user = users.findById(passkey.userId()).filter(User::active)
					.orElseThrow(PasskeyLoginFailedException::new);
			passkeys.recordUse(passkey.id(), verified.signCount(), verified.backedUp(), now);
			failures.recordSuccess(key, clientIp);
			return sessions.issue(user);
		}
		catch (InvalidPasskeyException e) {
			throw new PasskeyLoginFailedException();
		}
	}

	/** Passkey logins have no email: their attempts are counted per client. */
	private static String clientKey(String clientIp) {
		return "passkey|" + clientIp;
	}

}
