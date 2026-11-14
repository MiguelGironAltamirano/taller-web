package pe.edu.unmsm.servicios.dashboard.domain;

import java.time.Instant;
import java.time.LocalDate;

/** The {@code range} of the indicators: the last 7 or 30 days counting today, or the current month. */
public enum DashboardRange {

	LAST_7_DAYS, LAST_30_DAYS, CURRENT_MONTH;

	/** Parses the API value ({@code 7}, {@code 30} or {@code mes}); no value means the last 30 days. */
	public static DashboardRange parse(String value) {
		if (value == null || value.isBlank()) {
			return LAST_30_DAYS;
		}
		return switch (value) {
			case "7" -> LAST_7_DAYS;
			case "30" -> LAST_30_DAYS;
			case "mes" -> CURRENT_MONTH;
			default -> throw new InvalidDashboardQueryException("range", "El rango debe ser 7, 30 o mes");
		};
	}

	/** The period this range covers at {@code now}, with the days counted in Lima. */
	public ReportingPeriod periodAt(Instant now) {
		LocalDate today = LocalDate.ofInstant(now, ReportingPeriod.ZONE);
		return switch (this) {
			case LAST_7_DAYS -> new ReportingPeriod(today.minusDays(6), 7);
			case LAST_30_DAYS -> new ReportingPeriod(today.minusDays(29), 30);
			case CURRENT_MONTH -> new ReportingPeriod(today.withDayOfMonth(1), today.getDayOfMonth());
		};
	}

}
