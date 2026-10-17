package pe.edu.unmsm.servicios.catalog.domain;

/** A priority with the hours of attention (SLA) it grants. */
public record PriorityLevel(long id, String name, int slaHours) {
}
