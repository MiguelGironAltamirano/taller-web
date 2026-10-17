package pe.edu.unmsm.servicios.workflow.domain;

/** A technician as the workflow needs to see them to assign a request. */
public record Technician(long id, String fullName, Long areaId, boolean active) {
}
