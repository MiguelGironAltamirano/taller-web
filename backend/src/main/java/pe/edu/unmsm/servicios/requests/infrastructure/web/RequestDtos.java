package pe.edu.unmsm.servicios.requests.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import pe.edu.unmsm.servicios.requests.application.HistoryView;
import pe.edu.unmsm.servicios.requests.application.PageView;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.application.RequestSummaryView;

final class RequestDtos {

	private RequestDtos() {
	}

	/** The length of the description is a business rule checked (after trimming) by the domain. */
	record CreateRequestBody(
			@NotNull(message = "Selecciona una categoría") Long categoryId,
			@NotNull(message = "Selecciona un ambiente") Long locationId,
			@NotBlank(message = "La descripción es obligatoria") String description,
			@NotNull(message = "Selecciona una prioridad") Long priorityId) {
	}

	/** Contract summary plus the names the inboxes display (see docs/api/ui-extensions.md). */
	record SummaryResponse(String code, String categoryName, String locationName, String priorityName, String status,
			Instant createdAt, Instant dueAt, boolean overdue, String description, String requesterName,
			String areaName, String technicianName, String facultyName, String pavilionName, String roomName) {

		static SummaryResponse from(RequestSummaryView v) {
			return new SummaryResponse(v.code(), v.categoryName(), v.locationName(), v.priorityName(),
					v.status().name(), v.createdAt(), v.dueAt(), v.overdue(), v.description(), v.requesterName(),
					v.areaName(), v.technicianName(), v.facultyName(), v.pavilionName(), v.roomName());
		}

	}

	record PersonRef(long id, String fullName) {
	}

	record AreaRef(long id, String name) {
	}

	record HistoryResponse(String fromStatus, String toStatus, String actorName, String reason, Instant createdAt) {

		static HistoryResponse from(HistoryView h) {
			return new HistoryResponse(h.fromStatus() == null ? null : h.fromStatus().name(), h.toStatus().name(),
					h.actorName(), h.reason(), h.createdAt());
		}

	}

	/** {@code RequestDetail = RequestSummary & { … }} of the API contract, flattened. */
	record DetailResponse(String code, String categoryName, String locationName, String priorityName, String status,
			Instant createdAt, Instant dueAt, boolean overdue, String description, String requesterName,
			String areaName, String technicianName, String facultyName, String pavilionName, String roomName,
			PersonRef requester, AreaRef area, PersonRef technician, String closureReason,
			List<HistoryResponse> history, List<String> allowedTransitions) {

		static DetailResponse from(RequestDetailView d) {
			RequestSummaryView s = d.summary();
			PersonRef technician = d.technicianId() == null ? null
					: new PersonRef(d.technicianId(), s.technicianName());
			return new DetailResponse(s.code(), s.categoryName(), s.locationName(), s.priorityName(),
					s.status().name(), s.createdAt(), s.dueAt(), s.overdue(), s.description(), s.requesterName(),
					s.areaName(), s.technicianName(), s.facultyName(), s.pavilionName(), s.roomName(),
					new PersonRef(d.requesterId(), s.requesterName()), new AreaRef(d.areaId(), s.areaName()),
					technician, d.closureReason() == null ? null : d.closureReason().name(),
					d.history().stream().map(HistoryResponse::from).toList(),
					d.allowedTransitions().stream().map(Enum::name).toList());
		}

	}

	record PageResponse(List<SummaryResponse> items, int page, int size, long total) {

		static PageResponse from(PageView<RequestSummaryView> page) {
			return new PageResponse(page.items().stream().map(SummaryResponse::from).toList(), page.page(),
					page.size(), page.total());
		}

	}

}
