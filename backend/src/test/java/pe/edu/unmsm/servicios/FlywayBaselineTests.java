package pe.edu.unmsm.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayBaselineTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void appliesBaselineMigration() {
		Integer applied = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success = 1",
				Integer.class);
		assertThat(applied).isEqualTo(1);
	}

}
