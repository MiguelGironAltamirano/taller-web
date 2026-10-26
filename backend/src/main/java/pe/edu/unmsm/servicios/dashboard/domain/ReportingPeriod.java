package pe.edu.unmsm.servicios.dashboard.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * A run of whole days in Lima time, from {@code firstDay} for {@code days} days. A request belongs to the period
 * when it was registered in {@code [start, end)}.
 */
public record ReportingPeriod(LocalDate firstDay, int days) {

	/** The dashboard counts days in the time zone of the university, whatever the zone of the server is. */
	public static final ZoneId ZONE = ZoneId.of("America/Lima");

	public ReportingPeriod {
		if (firstDay == null || days < 1) {
			throw new IllegalArgumentException("A period needs a first day and at least one day");
		}
	}

	public LocalDate lastDay() {
		return firstDay.plusDays(days - 1L);
	}

	public Instant start() {
		return firstDay.atStartOfDay(ZONE).toInstant();
	}

	public Instant end() {
		return firstDay.plusDays(days).atStartOfDay(ZONE).toInstant();
	}

	/** The period of the same length that ends where this one starts. */
	public ReportingPeriod previous() {
		return new ReportingPeriod(firstDay.minusDays(days), days);
	}

	public boolean contains(Instant instant) {
		return dayIndexOf(instant) >= 0;
	}

	/** The position of the Lima day of {@code instant} in the period (0 = first day), or -1 when outside. */
	public int dayIndexOf(Instant instant) {
		long index = ChronoUnit.DAYS.between(firstDay, LocalDate.ofInstant(instant, ZONE));
		return index >= 0 && index < days ? (int) index : -1;
	}

}
