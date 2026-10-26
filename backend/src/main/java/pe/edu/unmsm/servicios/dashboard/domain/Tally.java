package pe.edu.unmsm.servicios.dashboard.domain;

/** How many requests share a label (a category, a priority, a technician). */
public record Tally(String label, long value) {
}
