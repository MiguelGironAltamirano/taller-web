package pe.edu.unmsm.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class SchemaMigrationTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void createsAllMvpTables() {
		List<String> tables = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = database()", String.class);
		assertThat(tables).contains("areas", "categories", "priorities", "locations", "users", "refresh_tokens",
				"request_code_sequences", "service_requests", "request_history");
	}

	@Test
	void seedsPrioritiesWithSlaHours() {
		Integer critical = jdbc.queryForObject("select sla_hours from priorities where name = 'Crítica'", Integer.class);
		Integer count = jdbc.queryForObject("select count(*) from priorities", Integer.class);
		assertThat(critical).isEqualTo(8);
		assertThat(count).isEqualTo(4);
	}

	@Test
	void everyCategoryHasADefaultArea() {
		Integer orphans = jdbc.queryForObject(
				"select count(*) from categories c left join areas a on a.id = c.default_area_id where a.id is null",
				Integer.class);
		assertThat(orphans).isZero();
	}

	@Test
	void devSeedCreatesOneUserPerRoleWithHashedPasswords() {
		List<String> roles = jdbc.queryForList("select role from users order by id", String.class);
		Integer plain = jdbc.queryForObject("select count(*) from users where password_hash not like '$2%'", Integer.class);
		assertThat(roles).containsExactly("ADMINISTRADOR", "SUPERVISOR", "TECNICO", "SOLICITANTE");
		assertThat(plain).isZero();
	}

}
