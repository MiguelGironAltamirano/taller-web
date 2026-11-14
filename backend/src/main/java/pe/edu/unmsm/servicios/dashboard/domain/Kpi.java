package pe.edu.unmsm.servicios.dashboard.domain;

import java.util.List;

/** An indicator: its value in the period, its change against the previous period and its value per day. */
public record Kpi(double value, double deltaPct, List<Double> series) {

	public Kpi {
		series = List.copyOf(series);
	}

}
