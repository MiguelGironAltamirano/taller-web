package pe.edu.unmsm.servicios.notifications.infrastructure.adapter;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AreaSupervisorLookup {

	private final JdbcTemplate jdbc;

	public AreaSupervisorLookup(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Long> findSupervisorIdsOfArea(long areaId) {
		String sql = "SELECT id FROM users WHERE role = 'SUPERVISOR' AND area_id = ? AND active = TRUE";
		return jdbc.query(sql, (rs, rowNum) -> rs.getLong("id"), areaId);
	}

}
