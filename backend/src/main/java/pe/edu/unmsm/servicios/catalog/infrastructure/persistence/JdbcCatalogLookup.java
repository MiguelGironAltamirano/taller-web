package pe.edu.unmsm.servicios.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.Area;
import pe.edu.unmsm.servicios.catalog.domain.Catalogs;
import pe.edu.unmsm.servicios.catalog.domain.Category;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.LocationKind;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;

/** Read-only reference data: plain SQL is simpler than JPA entities for these small lookup tables. */
@Repository
class JdbcCatalogLookup implements CatalogLookup {

	private final JdbcTemplate jdbc;

	JdbcCatalogLookup(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Catalogs activeCatalogs() {
		List<Area> areas = jdbc.query("select id, name from areas where active = true order by id",
				(rs, row) -> new Area(rs.getLong("id"), rs.getString("name")));
		List<Category> categories = jdbc.query(
				"select id, name, default_area_id from categories where active = true order by id",
				(rs, row) -> new Category(rs.getLong("id"), rs.getString("name"), rs.getLong("default_area_id")));
		List<PriorityLevel> priorities = jdbc.query(
				"select id, name, sla_hours from priorities order by sort_order, id",
				(rs, row) -> new PriorityLevel(rs.getLong("id"), rs.getString("name"), rs.getInt("sla_hours")));
		List<Location> locations = jdbc.query("select id, name, kind, parent_id from locations order by id",
				(rs, row) -> toLocation(rs));
		return new Catalogs(areas, categories, priorities, locations);
	}

	@Override
	public Optional<Category> findActiveCategory(long id) {
		return jdbc.query("select id, name, default_area_id from categories where id = ? and active = true",
				(rs, row) -> new Category(rs.getLong("id"), rs.getString("name"), rs.getLong("default_area_id")), id)
				.stream().findFirst();
	}

	@Override
	public Optional<PriorityLevel> findPriority(long id) {
		return jdbc.query("select id, name, sla_hours from priorities where id = ?",
				(rs, row) -> new PriorityLevel(rs.getLong("id"), rs.getString("name"), rs.getInt("sla_hours")), id)
				.stream().findFirst();
	}

	@Override
	public Optional<Location> findLocation(long id) {
		return jdbc.query("select id, name, kind, parent_id from locations where id = ?", (rs, row) -> toLocation(rs),
				id).stream().findFirst();
	}

	private static Location toLocation(java.sql.ResultSet rs) throws java.sql.SQLException {
		long parent = rs.getLong("parent_id");
		// wasNull() only refers to the last column read, so it has to be checked right after reading parent_id.
		Long parentId = rs.wasNull() ? null : parent;
		return new Location(rs.getLong("id"), rs.getString("name"), LocationKind.valueOf(rs.getString("kind")),
				parentId);
	}

}
