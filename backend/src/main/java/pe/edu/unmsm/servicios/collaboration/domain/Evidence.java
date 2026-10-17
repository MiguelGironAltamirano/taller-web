package pe.edu.unmsm.servicios.collaboration.domain;

import java.time.Instant;

public record Evidence(Long id, long requestId, long uploaderId, String fileName, String storedName,
		EvidenceKind kind, long sizeBytes, Instant createdAt) {
}
