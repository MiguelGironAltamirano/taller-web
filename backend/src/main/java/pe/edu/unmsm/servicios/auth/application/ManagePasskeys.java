package pe.edu.unmsm.servicios.auth.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.AuthenticatorSelection;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.CreationOptions;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.CredentialDescriptor;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.CredentialParameter;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.RelyingParty;
import pe.edu.unmsm.servicios.auth.application.PasskeyOptions.UserAccount;
import pe.edu.unmsm.servicios.auth.application.PasskeyVerifier.VerifiedRegistration;
import pe.edu.unmsm.servicios.auth.domain.ChallengePurpose;
import pe.edu.unmsm.servicios.auth.domain.InvalidCurrentPasswordException;
import pe.edu.unmsm.servicios.auth.domain.InvalidPasskeyException;
import pe.edu.unmsm.servicios.auth.domain.Passkey;
import pe.edu.unmsm.servicios.auth.domain.PasskeyLimitReachedException;
import pe.edu.unmsm.servicios.auth.domain.PasskeyNotFoundException;
import pe.edu.unmsm.servicios.auth.domain.User;

/** The user's own passkeys: register (confirming the password first), list, rename and delete. */
public class ManagePasskeys {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();
	/** ES256 and RS256 cover platform authenticators (Windows Hello, Touch ID, Android) and security keys. */
	private static final List<CredentialParameter> ALGORITHMS = List.of(new CredentialParameter("public-key", -7),
			new CredentialParameter("public-key", -257));
	private static final DateTimeFormatter DEFAULT_NAME_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
			.withZone(ZoneId.of("America/Lima"));

	private final PasskeyRepository passkeys;
	private final WebAuthnChallengeRepository challenges;
	private final WebAuthnUserHandles handles;
	private final UserRepository users;
	private final PasswordHasher hasher;
	private final PasskeyVerifier verifier;
	private final LoginAttemptLimiter limiter;
	private final PasskeySettings settings;
	private final Clock clock;

	public ManagePasskeys(PasskeyRepository passkeys, WebAuthnChallengeRepository challenges,
			WebAuthnUserHandles handles, UserRepository users, PasswordHasher hasher, PasskeyVerifier verifier,
			LoginAttemptLimiter limiter, PasskeySettings settings, Clock clock) {
		this.passkeys = passkeys;
		this.challenges = challenges;
		this.handles = handles;
		this.users = users;
		this.hasher = hasher;
		this.verifier = verifier;
		this.limiter = limiter;
		this.settings = settings;
		this.clock = clock;
	}

	/**
	 * Registration options after confirming the password: a stolen access token lasts 15 minutes, but a passkey
	 * registered with it would give permanent access. Wrong passwords count towards the user's login limit.
	 */
	public PasskeyOptions<CreationOptions> startRegistration(long userId, String currentPassword, String clientIp) {
		User user = users.findById(userId).orElseThrow(InvalidCurrentPasswordException::new);
		limiter.acquire(user.email(), clientIp);
		if (currentPassword == null || !hasher.matches(currentPassword, user.passwordHash())) {
			throw new InvalidCurrentPasswordException();
		}
		limiter.recordSuccess(user.email(), clientIp);
		List<Passkey> existing = passkeys.findByUser(userId);
		if (existing.size() >= settings.maxPasskeysPerUser()) {
			throw new PasskeyLimitReachedException(settings.maxPasskeysPerUser());
		}
		byte[] handle = handles.getOrCreate(userId);
		Instant now = clock.instant();
		challenges.deleteExpired(now);
		byte[] challenge = new byte[32];
		RANDOM.nextBytes(challenge);
		String id = UUID.randomUUID().toString();
		challenges.save(id, ChallengePurpose.REGISTRATION, userId, challenge, now.plus(settings.challengeTtl()));
		List<CredentialDescriptor> exclude = existing.stream()
				.map(p -> new CredentialDescriptor("public-key", BASE64URL.encodeToString(p.credentialId()),
						transports(p.transports())))
				.toList();
		return new PasskeyOptions<>(id, new CreationOptions(BASE64URL.encodeToString(challenge),
				new RelyingParty(settings.rpId(), settings.rpName()),
				new UserAccount(BASE64URL.encodeToString(handle), user.email(), user.fullName()), ALGORITHMS,
				PasskeySettings.CEREMONY_TIMEOUT_MILLIS, exclude,
				new AuthenticatorSelection("required", true, "required"), "none"));
	}

	/** Stores the passkey created with a registration challenge issued to this same user. */
	public Passkey register(long userId, String challengeId, String name, String responseJson) {
		Instant now = clock.instant();
		byte[] challenge = challenges.consume(challengeId, ChallengePurpose.REGISTRATION, userId, now)
				.orElseThrow(() -> new InvalidPasskeyException("Unknown, expired or used registration challenge"));
		VerifiedRegistration verified = verifier.verifyRegistration(responseJson, challenge);
		if (passkeys.countByUser(userId) >= settings.maxPasskeysPerUser()) {
			throw new PasskeyLimitReachedException(settings.maxPasskeysPerUser());
		}
		return passkeys.save(new Passkey(null, userId, verified.credentialId(), verified.publicKey(),
				verified.signCount(), verified.transports(), verified.backupEligible(), verified.backedUp(),
				nameOrDefault(name, now), now, null));
	}

	public List<Passkey> list(long userId) {
		return passkeys.findByUser(userId);
	}

	public Passkey rename(long userId, long id, String name) {
		return passkeys.rename(id, userId, name.strip()).orElseThrow(() -> new PasskeyNotFoundException(id));
	}

	/** Only future logins are affected: sessions already open last until they expire. */
	public void delete(long userId, long id) {
		if (!passkeys.delete(id, userId)) {
			throw new PasskeyNotFoundException(id);
		}
	}

	private static String nameOrDefault(String name, Instant now) {
		return name == null || name.isBlank() ? "Passkey del " + DEFAULT_NAME_DATE.format(now) : name.strip();
	}

	private static List<String> transports(String stored) {
		return stored == null || stored.isBlank() ? List.of() : Arrays.asList(stored.split(","));
	}

}
