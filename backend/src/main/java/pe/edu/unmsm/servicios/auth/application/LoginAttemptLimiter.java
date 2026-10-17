package pe.edu.unmsm.servicios.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pe.edu.unmsm.servicios.auth.domain.TooManyLoginAttemptsException;

/** In-memory limiter of failed logins per email (single instance deployments). */
public class LoginAttemptLimiter {

	private static final int SWEEP_THRESHOLD = 10_000;

	private final Clock clock;
	private final int maxFailures;
	private final Duration window;
	private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

	public LoginAttemptLimiter(Clock clock, int maxFailures, Duration window) {
		this.clock = clock;
		this.maxFailures = maxFailures;
		this.window = window;
	}

	public void checkAllowed(String email) {
		Deque<Instant> recent = failures.get(key(email));
		if (recent == null) {
			return;
		}
		Instant now = clock.instant();
		long retryAfter;
		synchronized (recent) {
			prune(recent, now);
			if (recent.size() < maxFailures) {
				return;
			}
			retryAfter = Math.max(1, (Duration.between(now, recent.peekFirst().plus(window)).toMillis() + 999) / 1000);
		}
		throw new TooManyLoginAttemptsException(retryAfter);
	}

	public void recordFailure(String email) {
		if (failures.size() > SWEEP_THRESHOLD) {
			sweep();
		}
		Deque<Instant> recent = failures.computeIfAbsent(key(email), k -> new ArrayDeque<>());
		Instant now = clock.instant();
		synchronized (recent) {
			prune(recent, now);
			recent.addLast(now);
		}
	}

	public void recordSuccess(String email) {
		failures.remove(key(email));
	}

	private void sweep() {
		Instant now = clock.instant();
		failures.entrySet().removeIf(entry -> {
			synchronized (entry.getValue()) {
				prune(entry.getValue(), now);
				return entry.getValue().isEmpty();
			}
		});
	}

	private void prune(Deque<Instant> recent, Instant now) {
		while (!recent.isEmpty() && !recent.peekFirst().plus(window).isAfter(now)) {
			recent.removeFirst();
		}
	}

	private static String key(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
