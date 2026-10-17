package pe.edu.unmsm.servicios.admin.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.admin.application.CatalogAdminStore;
import pe.edu.unmsm.servicios.admin.domain.AdminArea;
import pe.edu.unmsm.servicios.admin.domain.AdminCategory;
import pe.edu.unmsm.servicios.admin.domain.CatalogConflictException;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.LocationKind;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;

/** Plain SQL over the small catalog tables, like the catalog module's read side. */
@Repository
class JdbcCatalogAdminStore implements CatalogAdminStore {

	private final JdbcTemplate jdbc;

	JdbcCatalogAdminStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<AdminArea> findArea(long id) {
		return jdbc.query("select id, name, active from areas where id = ?", JdbcCatalogAdminStore::toArea, id)
				.stream().findFirst();
	}

	@Override
	public Optional<AdminArea> findAreaByName(String name) {
		return jdbc.query("select id, name, active from areas where name = ?", JdbcCatalogAdminStore::toArea, name)
				.stream().findFirst();
	}

	@Override
	public AdminArea insertArea(String name) {
		long id = insert("insert into areas (name, active) values (?, true)", "name",
				"Ya existe un área con ese nombre", name);
		return new AdminArea(id, name, true);
	}

	@Override
	public void setAreaActive(long id, boolean active) {
		jdbc.update("update areas set active = ? where id = ?", active, id);
	}

	@Override
	public boolean areaHasActiveCategories(long areaId) {
		return exists("select exists (select 1 from categories where default_area_id = ? and active = true)", areaId);
	}

	@Override
	public boolean areaHasActiveStaff(long areaId) {
		return exists("select exists (select 1 from users where area_id = ? and active = true"
				+ " and role in ('TECNICO', 'SUPERVISOR'))", areaId);
	}

	@Override
	public Optional<AdminCategory> findCategory(long id) {
		return jdbc.query("select id, name, default_area_id, active from categories where id = ?",
				JdbcCatalogAdminStore::toCategory, id).stream().findFirst();
	}

	@Override
	public Optional<AdminCategory> findCategoryByName(String name) {
		return jdbc.query("select id, name, default_area_id, active from categories where name = ?",
				JdbcCatalogAdminStore::toCategory, name).stream().findFirst();
	}

	@Override
	public AdminCategory insertCategory(String name, long areaId) {
		long id = insert("insert into categories (name, default_area_id, active) values (?, ?, true)", "name",
				"Ya existe una categoría con ese nombre", name, areaId);
		return new AdminCategory(id, name, areaId, true);
	}

	@Override
	public void updateCategory(long id, long areaId, boolean active) {
		jdbc.update("update categories set default_area_id = ?, active = ? where id = ?", areaId, active, id);
	}

	@Override
	public Optional<PriorityLevel> findPriority(long id) {
		return jdbc.query("select id, name, sla_hours from priorities where id = ?",
				(rs, row) -> new PriorityLevel(rs.getLong("id"), rs.getString("name"), rs.getInt("sla_hours")), id)
				.stream().findFirst();
	}

	@Override
	public void updatePrioritySla(long id, int slaHours) {
		jdbc.update("update priorities set sla_hours = ? where id = ?", slaHours, id);
	}

	@Override
	public Optional<Location> findLocation(long id) {
		return jdbc.query("select id, name, kind, parent_id from locations where id = ?",
				JdbcCatalogAdminStore::toLocation, id).stream().findFirst();
	}

	@Override
	public Optional<Location> findLocationByName(String name, LocationKind kind, Long parentId) {
		// <=> is MySQL's null-safe equality: faculties have no parent.
		return jdbc.query("select id, name, kind, parent_id from locations where name = ? and kind = ? and parent_id <=> ?",
				JdbcCatalogAdminStore::toLocation, name, kind.name(), parentId).stream().findFirst();
	}

	@Override
	public Location insertLocation(String name, LocationKind kind, Long parentId) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"insert into locations (name, kind, parent_id) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, name);
			statement.setString(2, kind.name());
			if (parentId == null) {
				statement.setNull(3, Types.BIGINT);
			}
			else {
				statement.setLong(3, parentId);
			}
			return statement;
		}, keys);
		return new Location(keys.getKey().longValue(), name, kind, parentId);
	}

	@Override
	public boolean locationHasChildren(long id) {
		return exists("select exists (select 1 from locations where parent_id = ?)", id);
	}

	@Override
	public boolean locationHasRequests(long id) {
		return exists("select exists (select 1 from service_requests where location_id = ?)", id);
	}

	@Override
	public void deleteLocation(long id) {
		jdbc.update("delete from locations where id = ?", id);
	}

	/** Inserts a row with a unique name; a concurrent duplicate surfaces as a conflict on {@code field}. */
	private long insert(String sql, String field, String duplicateMessage, Object... args) {
		KeyHolder keys = new GeneratedKeyHolder();
		try {
			jdbc.update(connection -> {
				PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				for (int i = 0; i < args.length; i++) {
					statement.setObject(i + 1, args[i]);
				}
				return statement;
			}, keys);
		}
		catch (DuplicateKeyException ex) {
			throw new CatalogConflictException(duplicateMessage, Map.of(field, duplicateMessage));
		}
		return keys.getKey().longValue();
	}

	private boolean exists(String sql, long id) {
		return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, id));
	}

	private static AdminArea toArea(ResultSet rs, int row) throws SQLException {
		return new AdminArea(rs.getLong("id"), rs.getString("name"), rs.getBoolean("active"));
	}

	private static AdminCategory toCategory(ResultSet rs, int row) throws SQLException {
		return new AdminCategory(rs.getLong("id"), rs.getString("name"), rs.getLong("default_area_id"),
				rs.getBoolean("active"));
	}

	private static Location toLocation(ResultSet rs, int row) throws SQLException {
		long parent = rs.getLong("parent_id");
		Long parentId = rs.wasNull() ? null : parent;
		return new Location(rs.getLong("id"), rs.getString("name"), LocationKind.valueOf(rs.getString("kind")),
				parentId);
	}

}
