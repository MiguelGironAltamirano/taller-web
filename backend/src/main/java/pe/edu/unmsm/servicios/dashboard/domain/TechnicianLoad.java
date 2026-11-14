package pe.edu.unmsm.servicios.dashboard.domain;

/** The current work of a technician against the reference capacity. */
public record TechnicianLoad(long id, String fullName, long assigned, long inProgress, long overdue, int capacity) {
}
