package pe.edu.unmsm.servicios.catalog.domain;

/** A request category; {@code areaId} is its default (and only) responsible area. */
public record Category(long id, String name, long areaId) {
}
