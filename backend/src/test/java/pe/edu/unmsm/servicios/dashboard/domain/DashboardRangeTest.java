package pe.edu.unmsm.servicios.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DashboardRangeTest {

	// 2026-10-26 10:00 in Lima (UTC-5).
	private static final Instant MORNING = Instant.parse("2026-10-26T15:00:00Z");
	// 2026-10-26 22:00 in Lima, already the 27th in UTC.
	private static final Instant NIGHT = Instant.parse("2026-10-27T03:00:00Z");

	@Test
	void parsesTheThreeContractValuesAndDefaultsToThirtyDays() {
		assertThat(DashboardRange.parse("7")).isEqualTo(DashboardRange.LAST_7_DAYS);
		assertThat(DashboardRange.parse("30")).isEqualTo(DashboardRange.LAST_30_DAYS);
		assertThat(DashboardRange.parse("mes")).isEqualTo(DashboardRange.CURRENT_MONTH);
		assertThat(DashboardRange.parse(null)).isEqualTo(DashboardRange.LAST_30_DAYS);
		assertThat(DashboardRange.parse("  ")).isEqualTo(DashboardRange.LAST_30_DAYS);
	}

	@Test
	void rejectsAnyOtherValue() {
		for (String value : new String[] { "15", "MES", "month", "-7", "7d" }) {
			assertThatThrownBy(() -> DashboardRange.parse(value)).isInstanceOf(InvalidDashboardQueryException.class)
					.satisfies(e -> assertThat(((InvalidDashboardQueryException) e).errors()).containsKey("range"));
		}
	}

	@Test
	void lastSevenDaysCountTodayInLima() {
		ReportingPeriod period = DashboardRange.LAST_7_DAYS.periodAt(MORNING);

		assertThat(period.firstDay()).isEqualTo(LocalDate.of(2026, 10, 20));
		assertThat(period.lastDay()).isEqualTo(LocalDate.of(2026, 10, 26));
		assertThat(period.days()).isEqualTo(7);
		assertThat(period.start()).isEqualTo(Instant.parse("2026-10-20T05:00:00Z"));
		assertThat(period.end()).isEqualTo(Instant.parse("2026-10-27T05:00:00Z"));
	}

	@Test
	void todayIsTheLimaDateEvenWhenUtcIsAlreadyTomorrow() {
		ReportingPeriod period = DashboardRange.LAST_30_DAYS.periodAt(NIGHT);

		assertThat(period.lastDay()).isEqualTo(LocalDate.of(2026, 10, 26));
		assertThat(period.firstDay()).isEqualTo(LocalDate.of(2026, 9, 27));
		assertThat(period.days()).isEqualTo(30);
	}

	@Test
	void currentMonthStartsOnTheFirstAndLastsUntilToday() {
		ReportingPeriod period = DashboardRange.CURRENT_MONTH.periodAt(MORNING);

		assertThat(period.firstDay()).isEqualTo(LocalDate.of(2026, 10, 1));
		assertThat(period.days()).isEqualTo(26);
	}

	@Test
	void currentMonthOnTheFirstDayHasOneDay() {
		ReportingPeriod period = DashboardRange.CURRENT_MONTH.periodAt(Instant.parse("2026-11-01T05:00:00Z"));

		assertThat(period.firstDay()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(period.days()).isEqualTo(1);
	}

	@Test
	void thePreviousPeriodHasTheSameLengthAndEndsWhereThisOneStarts() {
		ReportingPeriod period = DashboardRange.CURRENT_MONTH.periodAt(MORNING);

		ReportingPeriod previous = period.previous();

		assertThat(previous.days()).isEqualTo(26);
		assertThat(previous.firstDay()).isEqualTo(LocalDate.of(2026, 9, 5));
		assertThat(previous.end()).isEqualTo(period.start());
	}

	@Test
	void dayIndexFollowsLimaMidnight() {
		ReportingPeriod period = DashboardRange.LAST_7_DAYS.periodAt(MORNING);

		assertThat(period.dayIndexOf(Instant.parse("2026-10-20T04:59:59Z"))).isEqualTo(-1);
		assertThat(period.dayIndexOf(Instant.parse("2026-10-20T05:00:00Z"))).isZero();
		assertThat(period.dayIndexOf(Instant.parse("2026-10-21T04:59:59Z"))).isZero();
		assertThat(period.dayIndexOf(Instant.parse("2026-10-21T05:00:00Z"))).isEqualTo(1);
		assertThat(period.dayIndexOf(Instant.parse("2026-10-27T04:59:59Z"))).isEqualTo(6);
		assertThat(period.dayIndexOf(Instant.parse("2026-10-27T05:00:00Z"))).isEqualTo(-1);
		assertThat(period.contains(Instant.parse("2026-10-20T05:00:00Z"))).isTrue();
		assertThat(period.contains(Instant.parse("2026-10-20T04:59:59Z"))).isFalse();
	}

}
