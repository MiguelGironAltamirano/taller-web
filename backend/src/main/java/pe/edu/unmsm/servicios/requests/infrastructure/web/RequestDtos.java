package pe.edu.unmsm.servicios.requests.infrastructure.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import pe.edu.unmsm.servicios.requests.application.CommentView;
import pe.edu.unmsm.servicios.requests.application.EvidenceView;
import pe.edu.unmsm.servicios.requests.application.HistoryView;
import pe.edu.unmsm.servicios.requests.application.PageView;
import pe.edu.unmsm.servicios.requests.application.RequestCountsView;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.application.RequestSummaryView;
import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

public final class RequestDtos {

	private RequestDtos() {
	}

	/** The length of the description is a business rule checked (after trimming) by the domain. */
	record CreateRequestBody(
			@NotNull(message = "Selecciona una categoría") Long categoryId,
			@NotNull(message = "Selecciona un ambiente") Long locationId,
			@NotBlank(message = "La descripción es obligatoria") String description,
			@NotNull(message = "Selecciona una prioridad") Long priorityId) {
	}

	/** {@code { priorityId }} of {@code PATCH /requests/{code}/priority}. */
	record ChangePriorityBody(@NotNull(message = "Selecciona una prioridad") Long priorityId) {
	}

	/** Contract summary plus the names the inboxes display (see docs/api/ui-extensions.md). */
	record SummaryResponse(String code, String categoryName, String locationName, String priorityName, String status,
			Instant createdAt, Instant dueAt, boolean estimatedDue, boolean overdue, String description,
			String requesterName, String areaName, String technicianName, String facultyName, String pavilionName,
			String roomName) {

		static SummaryResponse from(RequestSummaryView v) {
			return new SummaryResponse(v.code(), v.categoryName(), v.locationName(), v.priorityName(),
					v.status().name(), v.createdAt(), v.dueAt(), v.estimatedDue(), v.overdue(), v.description(),
					v.requesterName(), v.areaName(), v.technicianName(), v.facultyName(), v.pavilionName(), v.roomName());
		}

	}

	public record PersonRef(long id, String fullName) {
	}

	public record AreaRef(long id, String name) {
	}

	/** {@code note} is null for state changes and describes the change otherwise (e.g. a new priority). */
	public record HistoryResponse(String fromStatus, String toStatus, String actorName, String reason,
			Instant createdAt, String note) {

		static HistoryResponse from(HistoryView h) {
			return new HistoryResponse(h.fromStatus() == null ? null : h.fromStatus().name(), h.toStatus().name(),
					h.actorName(), h.reason(), h.createdAt(), h.note());
		}

	}

	/**
	 * {@code RequestDetail = RequestSummary & { … }} of the API contract, flattened. Public because the workflow
	 * module answers its transitions with the updated detail.
	 */
	public record DetailResponse(String code, String categoryName, String locationName, String priorityName,
			String status, Instant createdAt, Instant dueAt, boolean estimatedDue, boolean overdue,
			String description, String requesterName, String areaName, String technicianName, String facultyName,
			String pavilionName, String roomName, PersonRef requester, AreaRef area, PersonRef technician, String closureReason,
			List<HistoryResponse> history, List<String> allowedTransitions, List<String> allowedActions,
			List<CommentResponse> comments, List<EvidenceResponse> evidences, String solution, RatingResponse rating,
			Instant autoCloseAt) {

		public static DetailResponse from(RequestDetailView d) {
			RequestSummaryView s = d.summary();
			PersonRef technician = d.technicianId() == null ? null
					: new PersonRef(d.technicianId(), s.technicianName());
			return new DetailResponse(s.code(), s.categoryName(), s.locationName(), s.priorityName(),
					s.status().name(), s.createdAt(), s.dueAt(), s.estimatedDue(), s.overdue(), s.description(),
					s.requesterName(), s.areaName(), s.technicianName(), s.facultyName(), s.pavilionName(), s.roomName(),
					new PersonRef(d.requesterId(), s.requesterName()), new AreaRef(d.areaId(), s.areaName()),
					technician, d.closureReason() == null ? null : d.closureReason().name(),
					d.history().stream().map(HistoryResponse::from).toList(),
					d.allowedTransitions().stream().map(Enum::name).toList(),
					d.allowedActions().stream().map(RequestAction::apiName).toList(),
					d.comments().stream().map(CommentResponse::from).toList(),
					d.evidences().stream().map(EvidenceResponse::from).toList(), d.solution(),
					d.rating() == null ? null : new RatingResponse(d.rating().score(), d.rating().comment()),
					d.autoCloseAt());
		}

	}

	/** {@code RatingDto} of the contract; null in the detail until the requester rates the request. */
	public record RatingResponse(int score, String comment) {
	}

	/** {@code CommentDto} of the contract (same shape as {@code GET /requests/{code}/comments}). */
	public record CommentResponse(long id, String authorName, String text, Instant createdAt) {

		static CommentResponse from(CommentView c) {
			return new CommentResponse(c.id(), c.authorName(), c.text(), c.createdAt());
		}

	}

	/** {@code EvidenceDto} of the contract (same shape as {@code GET /requests/{code}/evidences}). */
	public record EvidenceResponse(long id, String fileName, String kind, long sizeBytes) {

		static EvidenceResponse from(EvidenceView e) {
			return new EvidenceResponse(e.id(), e.fileName(), e.kind(), e.sizeBytes());
		}

	}

	/** {@code total} is kept from v1; {@code totalPages} lets clients render the pager without arithmetic. */
	record PageResponse(List<SummaryResponse> items, int page, int size, long total, int totalPages) {

		static PageResponse from(PageView<RequestSummaryView> page) {
			return new PageResponse(page.items().stream().map(SummaryResponse::from).toList(), page.page(),
					page.size(), page.total(), page.totalPages());
		}

	}

	/** {@code GET /requests/counts}: every status is present, with zero when there is none. */
	record CountsResponse(long total, long overdue, Map<String, Long> byStatus) {

		static CountsResponse from(RequestCountsView counts) {
			Map<String, Long> byStatus = new LinkedHashMap<>();
			for (RequestStatus status : RequestStatus.values()) {
				byStatus.put(status.name(), counts.byStatus().get(status));
			}
			return new CountsResponse(counts.total(), counts.overdue(), byStatus);
		}

	}

}
