package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.requests.application.RequestCodeGenerator;
import pe.edu.unmsm.servicios.requests.domain.RequestCode;

/**
 * Per-year sequence kept in {@code request_code_sequences}. One atomic upsert takes the row lock and
 * increments the counter, so concurrent creations are serialized and get distinct, consecutive numbers; the
 * lock is held until the creating transaction ends, which makes a rollback give the number back.
 */
@Repository
class JdbcRequestCodeGenerator implements RequestCodeGenerator {

	private final JdbcTemplate jdbc;

	JdbcRequestCodeGenerator(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * {@code LAST_INSERT_ID(expr)} is per connection, so both statements must share the transaction's
	 * connection: calling this outside a transaction would silently read someone else's value.
	 */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public RequestCode next(int year) {
		jdbc.update("""
				insert into request_code_sequences (seq_year, `last_value`) values (?, last_insert_id(1))
				on duplicate key update `last_value` = last_insert_id(`last_value` + 1)
				""", year);
		Integer sequence = jdbc.queryForObject("select last_insert_id()", Integer.class);
		return new RequestCode(year, sequence);
	}

}
