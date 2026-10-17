package pe.edu.unmsm.servicios.catalog.domain;

public record Location(long id, String name, LocationKind kind, Long parentId) {
}
