package pe.edu.unmsm.servicios.dashboard.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/** Computes the dashboard indicators from the requests of a scope, without any I/O. */
public final class IndicatorsCalculator {

	public static final String UNASSIGNED = "Sin asignar";
	static final int DUE_SOON_LIMIT = 10;
	static final Duration DUE_SOON_WINDOW = Duration.ofHours(24);

	private IndicatorsCalculator() {
	}

	/**
	 * @param period     the period of the query
	 * @param areaName   the name of the filtered area, or null for all areas
	 * @param registered the requests of the scope registered in this period or in the previous one; any other is
	 *                   ignored
	 * @param priorities the names of the priorities in their order
	 * @param open       the open requests of the scope, whatever their registration date (for {@code dueSoon})
	 * @param now        the moment of the query
	 */
	public static Indicators calculate(ReportingPeriod period, String areaName, List<TrackedRequest> registered,
			List<String> priorities, List<TrackedRequest> open, Instant now) {
		List<TrackedRequest> current = registeredIn(period, registered);
		List<TrackedRequest> previous = registeredIn(period.previous(), registered);
		List<List<TrackedRequest>> days = byDay(period, current);

		Kpis kpis = new Kpis(kpi(Metric.REGISTERED, current, previous, days), kpi(Metric.PENDING, current, previous, days),
				kpi(Metric.ATTENDED, current, previous, days), kpi(Metric.RESOLVED_PCT, current, previous, days),
				kpi(Metric.AVG_ATTENTION_HOURS, current, previous, days), kpi(Metric.OVERDUE, current, previous, days));

		List<Long> timeline = days.stream().map(day -> (long) day.size()).toList();
		List<TrackedRequest> attended = current.stream().filter(TrackedRequest::attended).toList();
		double slaCompliancePct = percentage(attended.stream().filter(TrackedRequest::attendedOnTime).count(),
				attended.size());

		return new Indicators(period.days(), areaName, kpis, descending(current, TrackedRequest::categoryName),
				byPriority(current, priorities),
				descending(current, r -> r.technicianName() == null ? UNASSIGNED : r.technicianName()), timeline,
				slaCompliancePct, dueSoon(open, now));
	}

	/** Open requests due in {@code (now, now + 24 h]}, earliest first, at most ten. */
	static List<TrackedRequest> dueSoon(List<TrackedRequest> open, Instant now) {
		Instant limit = now.plus(DUE_SOON_WINDOW);
		return open.stream()
				.filter(r -> r.open() && r.dueAt().isAfter(now) && !r.dueAt().isAfter(limit))
				.sorted(Comparator.comparing(TrackedRequest::dueAt).thenComparing(TrackedRequest::code))
				.limit(DUE_SOON_LIMIT)
				.toList();
	}

	private static List<TrackedRequest> registeredIn(ReportingPeriod period, List<TrackedRequest> requests) {
		return requests.stream().filter(r -> period.contains(r.createdAt())).toList();
	}

	private static List<List<TrackedRequest>> byDay(ReportingPeriod period, List<TrackedRequest> requests) {
		List<List<TrackedRequest>> days = new ArrayList<>();
		for (int i = 0; i < period.days(); i++) {
			days.add(new ArrayList<>());
		}
		requests.forEach(r -> days.get(period.dayIndexOf(r.createdAt())).add(r));
		return days;
	}

	private static Kpi kpi(Metric metric, List<TrackedRequest> current, List<TrackedRequest> previous,
			List<List<TrackedRequest>> days) {
		double value = metric.of(current);
		double before = metric.of(previous);
		double deltaPct = before == 0 ? 0 : round((value - before) / before * 100);
		return new Kpi(value, deltaPct, days.stream().map(metric::of).toList());
	}

	private static List<Tally> descending(List<TrackedRequest> requests, Function<TrackedRequest, String> label) {
		Map<String, Long> counts = new TreeMap<>();
		requests.forEach(r -> counts.merge(label.apply(r), 1L, Long::sum));
		return counts.entrySet().stream()
				.map(e -> new Tally(e.getKey(), e.getValue()))
				.sorted(Comparator.comparingLong(Tally::value).reversed().thenComparing(Tally::label))
				.toList();
	}

	private static List<Tally> byPriority(List<TrackedRequest> requests, List<String> priorities) {
		return priorities.stream()
				.map(name -> new Tally(name, requests.stream().filter(r -> name.equals(r.priorityName())).count()))
				.toList();
	}

	/** {@code part / total × 100} with one decimal; 0 when there is no total. */
	static double percentage(long part, long total) {
		return total == 0 ? 0 : round(part * 100.0 / total);
	}

	static double round(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	/** Each KPI as a function of a set of requests, so the same rule gives the value, the delta and the series. */
	private enum Metric {

		REGISTERED(List::size),
		PENDING(requests -> requests.stream().filter(TrackedRequest::open).count()),
		ATTENDED(requests -> requests.stream().filter(TrackedRequest::attended).count()),
		RESOLVED_PCT(requests -> percentage(requests.stream().filter(TrackedRequest::attended).count(),
				requests.size())),
		AVG_ATTENTION_HOURS(requests -> round(requests.stream().filter(TrackedRequest::attended)
				.mapToDouble(TrackedRequest::attentionHours).average().orElse(0))),
		OVERDUE(requests -> requests.stream().filter(TrackedRequest::overdue).count());

		private final ToDoubleFunction<List<TrackedRequest>> rule;

		Metric(ToDoubleFunction<List<TrackedRequest>> rule) {
			this.rule = rule;
		}

		double of(List<TrackedRequest> requests) {
			return rule.applyAsDouble(requests);
		}

	}

}
