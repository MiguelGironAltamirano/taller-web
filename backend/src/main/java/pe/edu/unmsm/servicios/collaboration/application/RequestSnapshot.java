package pe.edu.unmsm.servicios.collaboration.application;

public record RequestSnapshot(long id, String code, long requesterId, long areaId, Long technicianId) {
}
