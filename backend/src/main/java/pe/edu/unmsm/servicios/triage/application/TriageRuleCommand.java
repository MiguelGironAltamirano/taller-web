package pe.edu.unmsm.servicios.triage.application;

import java.util.List;

/** Raw body of a rule; nulls are reported as validation errors, {@code active} defaults to true. */
public record TriageRuleCommand(List<String> keywords, Long categoryId, Long priorityId, Boolean active) {
}
