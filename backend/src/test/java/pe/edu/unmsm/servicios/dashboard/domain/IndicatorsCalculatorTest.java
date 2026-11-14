package pe.edu.unmsm.servicios.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class IndicatorsCalculatorTest {

	// 2026-10-26 10:00 in Lima: the 7-day period is Oct 20..26 and the previous one Oct 13..19.
	private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");
	private static final ReportingPeriod WEEK = DashboardRange.LAST_7_DAYS.periodAt(NOW);
	private static final List<String> PRIORITIES = List.of("Crítica", "Alta", "Media", "Baja");

	/** A Lima wall-clock time as an instant. */
	private static Instant lima(String localDateTime) {
		return Instant.parse(localDateTime + "-05:00");
	}

	private static TrackedRequest open(String code, Instant createdAt, String category, String priority, Long techId,
			String tech, boolean overdue) {
		return new TrackedRequest(code, "ASIGNADA", category, priority, "Aula 101", techId, tech, createdAt,
				createdAt.plus(Duration.ofHours(24)), null, true, overdue);
	}

	private static TrackedRequest attended(String code, Instant createdAt, String category, String priority,
			Long techId, String tech, long hoursToResolve, long slaHours) {
		return new TrackedRequest(code, "RESUELTA", category, priority, "Aula 101", techId, tech, createdAt,
				createdAt.plus(Duration.ofHours(slaHours)), createdAt.plus(Duration.ofHours(hoursToResolve)), false,
				false);
	}

	private static TrackedRequest closedWithoutAttention(String code, Instant createdAt, String category,
			String priority, Long techId, String tech) {
		return new TrackedRequest(code, "CERRADA", category, priority, "Aula 101", techId, tech, createdAt,
				createdAt.plus(Duration.ofHours(72)), null, false, false);
	}

	private static List<TrackedRequest> scenario() {
		return List.of(
				// Current period.
				open("A", lima("2026-10-26T09:00:00"), "Electricidad", "Alta", 1L, "Ana", false),
				open("B", lima("2026-10-20T00:00:00"), "Electricidad", "Crítica", null, null, true),
				attended("C", lima("2026-10-22T08:00:00"), "Gasfitería", "Alta", 1L, "Ana", 10, 24),
				attended("D", lima("2026-10-22T10:00:00"), "Electricidad", "Media", 2L, "Beto", 80, 72),
				closedWithoutAttention("E", lima("2026-10-23T12:00:00"), "Gasfitería", "Media", 2L, "Beto"),
				// Previous period.
				open("P1", lima("2026-10-19T23:59:59"), "Electricidad", "Alta", null, null, true),
				attended("P2", lima("2026-10-15T12:00:00"), "Electricidad", "Alta", 1L, "Ana", 5, 24),
				// Before both periods: ignored.
				open("OLD", lima("2026-10-12T12:00:00"), "Mobiliario", "Baja", null, null, true));
	}

	private static Indicators calculate(List<TrackedRequest> registered, List<TrackedRequest> open) {
		return IndicatorsCalculator.calculate(WEEK, "Mantenimiento", registered, PRIORITIES, open, NOW);
	}

	@Test
	void kpiValuesCountOnlyTheRequestsRegisteredInThePeriod() {
		Kpis kpis = calculate(scenario(), List.of()).kpis();

		assertThat(kpis.registered().value()).isEqualTo(5);
		assertThat(kpis.pending().value()).isEqualTo(2);
		assertThat(kpis.attended().value()).isEqualTo(2);
		assertThat(kpis.resolvedPct().value()).isEqualTo(40.0);
		assertThat(kpis.avgAttentionHours().value()).isEqualTo(45.0);
		assertThat(kpis.overdue().value()).isEqualTo(1);
	}

	@Test
	void deltasCompareWithThePreviousPeriodOfTheSameLength() {
		Kpis kpis = calculate(scenario(), List.of()).kpis();

		assertThat(kpis.registered().deltaPct()).isEqualTo(150.0);
		assertThat(kpis.pending().deltaPct()).isEqualTo(100.0);
		assertThat(kpis.attended().deltaPct()).isEqualTo(100.0);
		assertThat(kpis.resolvedPct().deltaPct()).isEqualTo(-20.0);
		assertThat(kpis.avgAttentionHours().deltaPct()).isEqualTo(800.0);
		assertThat(kpis.overdue().deltaPct()).isEqualTo(0.0);
	}

	@Test
	void deltaIsZeroWhenThePreviousPeriodIsZero() {
		Kpis kpis = calculate(List.of(open("A", lima("2026-10-26T09:00:00"), "Electricidad", "Alta", 1L, "Ana",
				false)), List.of()).kpis();

		assertThat(kpis.registered().value()).isEqualTo(1);
		assertThat(kpis.registered().deltaPct()).isZero();
	}

	@Test
	void deltaIsRoundedToOneDecimal() {
		List<TrackedRequest> requests = new ArrayList<>();
		requests.add(open("A", lima("2026-10-26T09:00:00"), "Electricidad", "Alta", null, null, false));
		for (int i = 0; i < 3; i++) {
			requests.add(open("P" + i, lima("2026-10-15T09:00:00"), "Electricidad", "Alta", null, null, false));
		}

		assertThat(calculate(requests, List.of()).kpis().registered().deltaPct()).isEqualTo(-66.7);
	}

	@Test
	void percentagesAndAveragesAreRoundedToOneDecimal() {
		List<TrackedRequest> requests = List.of(
				attended("A", lima("2026-10-21T09:00:00"), "Electricidad", "Alta", 1L, "Ana", 1, 24),
				attended("B", lima("2026-10-21T09:00:00"), "Electricidad", "Alta", 1L, "Ana", 2, 24),
				attended("C", lima("2026-10-21T09:00:00"), "Electricidad", "Alta", 1L, "Ana", 2, 1),
				open("D", lima("2026-10-21T09:00:00"), "Electricidad", "Alta", 1L, "Ana", false),
				open("E", lima("2026-10-21T09:00:00"), "Electricidad", "Alta", 1L, "Ana", false),
				open("F", lima("2026-10-21T09:00:00"), "Electricidad", "Alta", 1L, "Ana", false));

		Indicators indicators = calculate(requests, List.of());

		assertThat(indicators.kpis().resolvedPct().value()).isEqualTo(50.0);
		assertThat(indicators.kpis().avgAttentionHours().value()).isEqualTo(1.7);
		assertThat(indicators.slaCompliancePct()).isEqualTo(66.7);
	}

	@Test
	void seriesHaveOneValuePerDayOldestFirst() {
		Indicators indicators = calculate(scenario(), List.of());
		Kpis kpis = indicators.kpis();

		assertThat(kpis.registered().series()).containsExactly(1.0, 0.0, 2.0, 1.0, 0.0, 0.0, 1.0);
		assertThat(kpis.pending().series()).containsExactly(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0);
		assertThat(kpis.attended().series()).containsExactly(0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0);
		assertThat(kpis.resolvedPct().series()).containsExactly(0.0, 0.0, 100.0, 0.0, 0.0, 0.0, 0.0);
		assertThat(kpis.avgAttentionHours().series()).containsExactly(0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0);
		assertThat(kpis.overdue().series()).containsExactly(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		assertThat(indicators.timeline()).containsExactly(1L, 0L, 2L, 1L, 0L, 0L, 1L);
	}

	@Test
	void seriesLengthMatchesTheRangeDays() {
		for (DashboardRange range : DashboardRange.values()) {
			ReportingPeriod period = range.periodAt(NOW);

			Indicators indicators = IndicatorsCalculator.calculate(period, null, List.of(), PRIORITIES, List.of(),
					NOW);

			assertThat(indicators.rangeDays()).isEqualTo(period.days());
			assertThat(indicators.timeline()).hasSize(period.days());
			assertThat(indicators.kpis().registered().series()).hasSize(period.days());
			assertThat(indicators.kpis().avgAttentionHours().series()).hasSize(period.days());
		}
	}

	@Test
	void distributionsAreSortedAndPrioritiesKeepTheirOrderWithZeros() {
		Indicators indicators = calculate(scenario(), List.of());

		assertThat(indicators.byCategory()).containsExactly(new Tally("Electricidad", 3), new Tally("Gasfitería", 2));
		assertThat(indicators.byPriority()).containsExactly(new Tally("Crítica", 1), new Tally("Alta", 2),
				new Tally("Media", 2), new Tally("Baja", 0));
		assertThat(indicators.byTechnician()).containsExactly(new Tally("Ana", 2), new Tally("Beto", 2),
				new Tally("Sin asignar", 1));
	}

	@Test
	void slaComplianceIsTheShareOfAttendedResolvedBeforeTheirDueDate() {
		assertThat(calculate(scenario(), List.of()).slaCompliancePct()).isEqualTo(50.0);
	}

	@Test
	void anEmptyScopeGivesZerosEverywhere() {
		Indicators indicators = calculate(List.of(), List.of());

		Kpis kpis = indicators.kpis();
		for (Kpi kpi : List.of(kpis.registered(), kpis.pending(), kpis.attended(), kpis.resolvedPct(),
				kpis.avgAttentionHours(), kpis.overdue())) {
			assertThat(kpi.value()).isZero();
			assertThat(kpi.deltaPct()).isZero();
			assertThat(kpi.series()).hasSize(7).containsOnly(0.0);
		}
		assertThat(indicators.byCategory()).isEmpty();
		assertThat(indicators.byTechnician()).isEmpty();
		assertThat(indicators.byPriority()).extracting(Tally::value).containsOnly(0L);
		assertThat(indicators.timeline()).containsOnly(0L);
		assertThat(indicators.slaCompliancePct()).isZero();
		assertThat(indicators.dueSoon()).isEmpty();
		assertThat(indicators.areaName()).isEqualTo("Mantenimiento");
	}

	@Test
	void dueSoonListsOpenRequestsDueInTheNext24HoursSortedByDueDate() {
		List<TrackedRequest> open = List.of(due("LATER", 23), due("PAST", -1), due("SOON", 1), due("EDGE", 24),
				due("TOO_FAR", 25));

		assertThat(calculate(List.of(), open).dueSoon()).extracting(TrackedRequest::code).containsExactly("SOON",
				"LATER", "EDGE");
	}

	@Test
	void dueSoonIsLimitedToTen() {
		List<TrackedRequest> open = new ArrayList<>();
		for (int i = 12; i >= 1; i--) {
			open.add(due("R" + i, i));
		}

		assertThat(calculate(List.of(), open).dueSoon()).extracting(TrackedRequest::code).containsExactly("R1", "R2",
				"R3", "R4", "R5", "R6", "R7", "R8", "R9", "R10");
	}

	@Test
	void dueSoonIgnoresRequestsThatAreNoLongerOpen() {
		TrackedRequest resolved = new TrackedRequest("DONE", "RESUELTA", "Electricidad", "Alta", "Aula 101", 1L,
				"Ana", NOW.minus(Duration.ofHours(10)), NOW.plus(Duration.ofHours(2)), NOW, false, false);

		assertThat(calculate(List.of(), List.of(resolved)).dueSoon()).isEmpty();
	}

	private static TrackedRequest due(String code, long hoursFromNow) {
		Instant dueAt = NOW.plus(Duration.ofHours(hoursFromNow));
		return new TrackedRequest(code, "ASIGNADA", "Electricidad", "Alta", "Aula 101", 1L, "Ana",
				dueAt.minus(Duration.ofHours(24)), dueAt, null, true, hoursFromNow < 0);
	}

}
