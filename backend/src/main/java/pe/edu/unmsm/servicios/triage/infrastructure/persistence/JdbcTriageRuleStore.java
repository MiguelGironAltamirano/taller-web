package pe.edu.unmsm.servicios.triage.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.triage.application.TriageRuleStore;
import pe.edu.unmsm.servicios.triage.application.TriageRuleView;
import pe.edu.unmsm.servicios.triage.domain.TriageRule;

/**
 * Plain SQL over {@code triage_rules} and its keywords. The admin views join the catalog tables only to read the
 * names (also of inactive categories), the same read-model approach as the request queries; writes are validated
 * through {@code CatalogLookup} before reaching this store.
 */
@Repository
class JdbcTriageRuleStore implements TriageRuleStore {

	private static final String VIEW_SELECT = """
			select r.id, r.category_id, c.name as category_name, r.priority_id, p.name as priority_name, r.active
			  from triage_rules r
			  join categories c on c.id = r.category_id
			  join priorities p on p.id = r.priority_id
			""";

	private final JdbcTemplate jdbc;

	JdbcTriageRuleStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<TriageRuleView> findAll() {
		Map<Long, List<String>> keywords = keywordsByRule(
				"select rule_id, keyword from triage_rule_keywords order by rule_id, sort_order");
		return jdbc.query(VIEW_SELECT + " order by r.id", (rs, row) -> new TriageRuleView(rs.getLong("id"),
				keywords.getOrDefault(rs.getLong("id"), List.of()), rs.getLong("category_id"),
				rs.getString("category_name"), rs.getLong("priority_id"), rs.getString("priority_name"),
				rs.getBoolean("active")));
	}

	@Override
	public Optional<TriageRuleView> find(long id) {
		List<String> keywords = jdbc.queryForList(
				"select keyword from triage_rule_keywords where rule_id = ? order by sort_order", String.class, id);
		return jdbc.query(VIEW_SELECT + " where r.id = ?", (rs, row) -> new TriageRuleView(rs.getLong("id"), keywords,
				rs.getLong("category_id"), rs.getString("category_name"), rs.getLong("priority_id"),
				rs.getString("priority_name"), rs.getBoolean("active")), id).stream().findFirst();
	}

	@Override
	public List<TriageRule> findActive() {
		Map<Long, List<String>> keywords = keywordsByRule("select k.rule_id, k.keyword from triage_rule_keywords k"
				+ " join triage_rules r on r.id = k.rule_id where r.active = true order by k.rule_id, k.sort_order");
		return jdbc.query("select id, category_id, priority_id from triage_rules where active = true order by id",
				(rs, row) -> new TriageRule(rs.getLong("id"), keywords.getOrDefault(rs.getLong("id"), List.of()),
						rs.getLong("category_id"), rs.getLong("priority_id"), true));
	}

	@Override
	public boolean exists(long id) {
		Integer count = jdbc.queryForObject("select count(*) from triage_rules where id = ?", Integer.class, id);
		return count != null && count > 0;
	}

	@Override
	public long insert(TriageRule rule) {
		KeyHolder key = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"insert into triage_rules (category_id, priority_id, active) values (?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, rule.categoryId());
			statement.setLong(2, rule.priorityId());
			statement.setBoolean(3, rule.active());
			return statement;
		}, key);
		long id = key.getKey().longValue();
		insertKeywords(id, rule.keywords());
		return id;
	}

	@Override
	public void update(TriageRule rule) {
		jdbc.update("update triage_rules set category_id = ?, priority_id = ?, active = ? where id = ?",
				rule.categoryId(), rule.priorityId(), rule.active(), rule.id());
		jdbc.update("delete from triage_rule_keywords where rule_id = ?", rule.id());
		insertKeywords(rule.id(), rule.keywords());
	}

	@Override
	public void delete(long id) {
		// The keywords go with the rule (on delete cascade).
		jdbc.update("delete from triage_rules where id = ?", id);
	}

	private void insertKeywords(long ruleId, List<String> keywords) {
		List<Object[]> rows = new ArrayList<>();
		for (int i = 0; i < keywords.size(); i++) {
			rows.add(new Object[] { ruleId, i, keywords.get(i) });
		}
		jdbc.batchUpdate("insert into triage_rule_keywords (rule_id, sort_order, keyword) values (?, ?, ?)", rows);
	}

	private Map<Long, List<String>> keywordsByRule(String sql) {
		Map<Long, List<String>> byRule = new HashMap<>();
		jdbc.query(sql, rs -> {
			byRule.computeIfAbsent(rs.getLong(1), ruleId -> new ArrayList<>()).add(rs.getString(2));
		});
		return byRule;
	}

}
