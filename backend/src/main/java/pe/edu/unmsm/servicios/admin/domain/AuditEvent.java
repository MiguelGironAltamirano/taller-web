package pe.edu.unmsm.servicios.admin.domain;

import java.time.Instant;

/** One change on a request as shown in the administration audit log. */
public record AuditEvent(long id, Instant createdAt, String actorName, String requestCode, String detail) {
}
