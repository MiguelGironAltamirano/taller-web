package pe.edu.unmsm.servicios.dashboard.domain;

/** The six headline indicators of the dashboard. */
public record Kpis(Kpi registered, Kpi pending, Kpi attended, Kpi resolvedPct, Kpi avgAttentionHours, Kpi overdue) {
}
