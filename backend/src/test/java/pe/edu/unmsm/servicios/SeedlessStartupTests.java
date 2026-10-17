package pe.edu.unmsm.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** With the default Flyway locations (no db/seed-dev) no demo users may exist. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SeedlessStartupTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void demoUsersAreNotCreatedWithDefaultFlywayLocations() {
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isZero();
	}

}
