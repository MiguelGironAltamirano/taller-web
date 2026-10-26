package pe.edu.unmsm.servicios.requests.application;

/** An evidence as listed in the request detail ({@code EvidenceDto}); {@code kind} is {@code IMG} or {@code PDF}. */
public record EvidenceView(long id, String fileName, String kind, long sizeBytes) {
}
