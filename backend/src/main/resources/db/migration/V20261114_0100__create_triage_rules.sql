CREATE TABLE triage_rules (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  priority_id BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_triage_rules_category FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT fk_triage_rules_priority FOREIGN KEY (priority_id) REFERENCES priorities (id)
);

CREATE TABLE triage_rule_keywords (
  rule_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  keyword VARCHAR(50) NOT NULL,
  PRIMARY KEY (rule_id, sort_order),
  CONSTRAINT fk_triage_rule_keywords_rule FOREIGN KEY (rule_id) REFERENCES triage_rules (id) ON DELETE CASCADE
);
