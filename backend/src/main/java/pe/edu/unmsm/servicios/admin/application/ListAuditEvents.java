package pe.edu.unmsm.servicios.admin.application;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pe.edu.unmsm.servicios.admin.domain.AuditEvent;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryCriteria;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryQuery;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryRecord;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** Audit log derived from the request change history: newest first, at most {@value #LIMIT} events. */
public class ListAuditEvents {

	static final int LIMIT = 40;

	private static final String NO_STATE = "—";

	private static final Map<RequestStatus, String> LABELS = new EnumMap<>(Map.of(
			RequestStatus.REGISTRADA, "Registrada",
			RequestStatus.EN_EVALUACION, "En evaluación",
			RequestStatus.ASIGNADA, "Asignada",
			RequestStatus.EN_ATENCION, "En atención",
			RequestStatus.RESUELTA, "Resuelta",
			RequestStatus.CERRADA, "Cerrada"));

	private final RequestHistoryQuery history;

	public ListAuditEvents(RequestHistoryQuery history) {
		this.history = history;
	}

	/** {@code q} matches the actor, the request code, the reason or a state label; blank lists everything. */
	public List<AuditEvent> execute(String q) {
		String text = q == null ? "" : q.strip();
		return history.latest(new RequestHistoryCriteria(text, statusesMatching(text), LIMIT)).stream()
				.map(ListAuditEvents::toEvent)
				.toList();
	}

	private static Set<RequestStatus> statusesMatching(String text) {
		if (text.isEmpty()) {
			return Set.of();
		}
		String needle = normalize(text);
		return Arrays.stream(RequestStatus.values())
				.filter(status -> normalize(LABELS.get(status)).contains(needle))
				.collect(Collectors.toUnmodifiableSet());
	}

	private static AuditEvent toEvent(RequestHistoryRecord change) {
		String from = change.fromStatus() == null ? NO_STATE : LABELS.get(change.fromStatus());
		String detail = from + " → " + LABELS.get(change.toStatus());
		if (change.reason() != null && !change.reason().isBlank()) {
			detail += " · " + change.reason();
		}
		return new AuditEvent(change.id(), change.createdAt(), change.actorName(), change.requestCode(), detail);
	}

	private static String normalize(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

}
