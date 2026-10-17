package pe.edu.unmsm.servicios.workflow.application;

/** A technician as listed to the supervisor, with the requests they currently have open. */
public record TechnicianView(long id, String fullName, long activeRequests) {
}
