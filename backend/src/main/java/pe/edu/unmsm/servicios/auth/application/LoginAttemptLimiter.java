package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import pe.edu.unmsm.servicios.auth.domain.TooManyLoginAttemptsException;

/**
 * In-memory limiter of login attempts (single instance deployments).
 *
 * <p>Each attempt is counted when it is acquired, before the password is verified, so concurrent
 * attempts cannot slip past the limit. Counters are kept per email and client and per email alone;
 * the per-client limit stops one source from locking an account for everyone else, and the higher
 * per-email ceiling still bounds distributed guessing.
 */
public class LoginAttemptLimiter {

	private final Clock clock;
	private final int maxPerClient;
	private final int maxPerEmail;
	private final Duration window;
	private final int maxTrackedKeys;
	/** Insertion ordered: the eldest entry is the least recently updated one. Guarded by {@code this}. */
	private final Map<String, Deque<Instant>> attempts = new LinkedHashMap<>();

	public LoginAttemptLimiter(Clock clock, int maxPerClient, int maxPerEmail, Duration window, int maxTrackedKeys) {
		this.clock = clock;
		this.maxPerClient = maxPerClient;
		this.maxPerEmail = maxPerEmail;
		this.window = window;
		this.maxTrackedKeys = maxTrackedKeys;
	}

	/** Atomically checks both limits and, when allowed, counts the attempt. */
	public synchronized void acquire(String email, String clientIp) {
		Instant now = clock.instant();
		String clientKey = "c|" + normalize(email) + "|" + clientIp;
		String emailKey = "e|" + normalize(email);
		Deque<Instant> client = live(clientKey, now);
		Deque<Instant> perEmail = live(emailKey, now);
		long retryAfter = 0;
		if (client != null && client.size() >= maxPerClient) {
			retryAfter = secondsUntilFree(client, now);
		}
		if (perEmail != null && perEmail.size() >= maxPerEmail) {
			retryAfter = Math.max(retryAfter, secondsUntilFree(perEmail, now));
		}
		if (retryAfter > 0) {
			throw new TooManyLoginAttemptsException(retryAfter);
		}
		record(clientKey, client, now);
		record(emailKey, perEmail, now);
		evictOverCap();
	}

	public synchronized void recordSuccess(String email, String clientIp) {
		attempts.remove("c|" + normalize(email) + "|" + clientIp);
		attempts.remove("e|" + normalize(email));
	}

	synchronized int trackedKeys() {
		return attempts.size();
	}

	/** Returns the unexpired attempts of a key, dropping the key when its window has fully passed. */
	private Deque<Instant> live(String key, Instant now) {
		Deque<Instant> recent = attempts.get(key);
		if (recent == null) {
			return null;
		}
		while (!recent.isEmpty() && !recent.peekFirst().plus(window).isAfter(now)) {
			recent.removeFirst();
		}
		if (recent.isEmpty()) {
			attempts.remove(key);
			return null;
		}
		return recent;
	}

	private void record(String key, Deque<Instant> existing, Instant now) {
		Deque<Instant> recent = existing != null ? existing : new ArrayDeque<>();
		recent.addLast(now);
		attempts.remove(key);
		attempts.put(key, recent);
	}

	private void evictOverCap() {
		Iterator<String> eldest = attempts.keySet().iterator();
		while (attempts.size() > maxTrackedKeys && eldest.hasNext()) {
			eldest.next();
			eldest.remove();
		}
	}

	private long secondsUntilFree(Deque<Instant> recent, Instant now) {
		long millis = Duration.between(now, recent.peekFirst().plus(window)).toMillis();
		return Math.max(1, (millis + 999) / 1000);
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
