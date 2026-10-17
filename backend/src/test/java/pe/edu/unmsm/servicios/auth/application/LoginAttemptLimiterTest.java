package pe.edu.unmsm.servicios.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.auth.domain.TooManyLoginAttemptsException;

class LoginAttemptLimiterTest {

	static final class MutableClock extends Clock {

		private Instant now = Instant.parse("2026-01-01T00:00:00Z");

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

	private final MutableClock clock = new MutableClock();

	private LoginAttemptLimiter limiter(int perClient, int perEmail, int maxKeys) {
		return new LoginAttemptLimiter(clock, perClient, perEmail, Duration.ofMinutes(15), maxKeys);
	}

	private static void attempts(LoginAttemptLimiter limiter, String email, String ip, int count) {
		for (int i = 0; i < count; i++) {
			limiter.acquire(email, ip);
		}
	}

	@Test
	void failuresFromOneClientDoNotBlockTheSameEmailFromAnotherClient() {
		LoginAttemptLimiter limiter = limiter(5, 20, 100);
		attempts(limiter, "a@unmsm.edu.pe", "10.0.0.1", 5);

		assertThatThrownBy(() -> limiter.acquire("a@unmsm.edu.pe", "10.0.0.1"))
				.isInstanceOf(TooManyLoginAttemptsException.class);
		assertThatCode(() -> limiter.acquire("A@unmsm.edu.pe", "10.0.0.2")).doesNotThrowAnyException();
	}

	@Test
	void perEmailCeilingBlocksEveryClientOnceReached() {
		LoginAttemptLimiter limiter = limiter(5, 20, 100);
		for (int ip = 1; ip <= 4; ip++) {
			attempts(limiter, "a@unmsm.edu.pe", "10.0.0." + ip, 5);
		}

		assertThatThrownBy(() -> limiter.acquire("a@unmsm.edu.pe", "10.0.0.99"))
				.isInstanceOf(TooManyLoginAttemptsException.class);
	}

	@Test
	void successResetsCountersAndWindowExpiresThem() {
		LoginAttemptLimiter limiter = limiter(5, 20, 100);
		attempts(limiter, "a@unmsm.edu.pe", "10.0.0.1", 4);
		limiter.recordSuccess("a@unmsm.edu.pe", "10.0.0.1");
		attempts(limiter, "a@unmsm.edu.pe", "10.0.0.1", 5);
		clock.advance(Duration.ofMinutes(15).plusSeconds(1));

		assertThatCode(() -> limiter.acquire("a@unmsm.edu.pe", "10.0.0.1")).doesNotThrowAnyException();
	}

	@Test
	void trackedKeysAreCappedDroppingTheOldest() {
		LoginAttemptLimiter limiter = limiter(1, 100, 4);
		for (int i = 0; i < 50; i++) {
			limiter.acquire("u" + i + "@unmsm.edu.pe", "10.0.0.1");
		}

		assertThat(limiter.trackedKeys()).isLessThanOrEqualTo(4);
		// the oldest client was evicted, so it can attempt again; the newest is still limited
		assertThatCode(() -> limiter.acquire("u0@unmsm.edu.pe", "10.0.0.1")).doesNotThrowAnyException();
		assertThatThrownBy(() -> limiter.acquire("u49@unmsm.edu.pe", "10.0.0.1"))
				.isInstanceOf(TooManyLoginAttemptsException.class);
	}

}
