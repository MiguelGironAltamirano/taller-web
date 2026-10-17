package pe.edu.unmsm.servicios.collaboration.application;

public record DownloadedEvidence(String fileName, String contentType, byte[] content) {
}
