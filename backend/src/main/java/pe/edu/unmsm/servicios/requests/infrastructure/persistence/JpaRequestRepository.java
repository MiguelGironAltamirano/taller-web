package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.RequestRepository;
import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.domain.Cancellation;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;

@Repository
class JpaRequestRepository implements RequestRepository, RequestStateStore {

	private final SpringDataServiceRequestRepository requests;
	private final SpringDataRequestHistoryRepository history;

	JpaRequestRepository(SpringDataServiceRequestRepository requests, SpringDataRequestHistoryRepository history) {
		this.requests = requests;
		this.history = history;
	}

	/** Runs inside the use case's transaction: the request and its first history entry commit together. */
	@Override
	public ServiceRequest save(ServiceRequest request, RequestHistoryEntry firstEntry) {
		ServiceRequestEntity entity = new ServiceRequestEntity();
		entity.code = request.code();
		entity.requesterId = request.requesterId();
		entity.categoryId = request.categoryId();
		entity.locationId = request.locationId();
		entity.description = request.description();
		entity.areaId = request.areaId();
		entity.createdAt = request.createdAt();
		entity.requestedPriorityId = request.priorityId();
		entity.requestedDueAt = request.dueAt();
		copyMutableState(request, entity);
		ServiceRequestEntity stored = requests.saveAndFlush(entity);
		appendHistory(stored.id, firstEntry);
		return toDomain(stored);
	}

	@Override
	public Optional<ServiceRequest> findByCodeForUpdate(String code) {
		return requests.findByCode(code).map(JpaRequestRepository::toDomain);
	}

	/**
	 * Runs inside the use case's transaction, after {@link #findByCodeForUpdate}: the row is already locked and
	 * managed, so the changes and the new history entry commit together.
	 */
	@Override
	public ServiceRequest update(ServiceRequest request, RequestHistoryEntry entry) {
		ServiceRequestEntity entity = requests.findById(request.id()).orElseThrow();
		// Restoring a cancelled request moves its due date by the time it was cancelled; the requester's estimated
		// date moves just as much (contract «Cancelar y restaurar una solicitud»). Nothing else changes it.
		if (entry.fromStatus() == RequestStatus.CANCELADA && entity.requestedDueAt != null) {
			entity.requestedDueAt = entity.requestedDueAt.plus(java.time.Duration.between(entity.dueAt, request.dueAt()));
		}
		copyMutableState(request, entity);
		ServiceRequestEntity stored = requests.saveAndFlush(entity);
		appendHistory(stored.id, entry);
		return toDomain(stored);
	}

	@Override
	public Optional<Cancellation> lastCancellation(long requestId) {
		return history.findFirstByRequestIdAndToStatusOrderByCreatedAtDescIdDesc(requestId, RequestStatus.CANCELADA.name())
				.filter(row -> row.fromStatus != null)
				.map(row -> new Cancellation(RequestStatus.valueOf(row.fromStatus), row.createdAt));
	}

	/** Everything that may change after the registration (creation data never does). */
	private static void copyMutableState(ServiceRequest request, ServiceRequestEntity entity) {
		entity.priorityId = request.priorityId();
		entity.status = request.status().name();
		entity.technicianId = request.technicianId();
		entity.closureReason = request.closureReason() == null ? null : request.closureReason().name();
		entity.solution = request.solution();
		entity.dueAt = request.dueAt();
		entity.updatedAt = request.updatedAt();
	}

	private void appendHistory(long requestId, RequestHistoryEntry entry) {
		RequestHistoryEntity row = new RequestHistoryEntity();
		row.requestId = requestId;
		row.fromStatus = entry.fromStatus() == null ? null : entry.fromStatus().name();
		row.toStatus = entry.toStatus().name();
		row.actorId = entry.actorId();
		row.technicianId = entry.technicianId();
		row.reason = entry.reason();
		row.note = entry.note();
		row.internal = entry.internal();
		row.createdAt = entry.createdAt();
		history.saveAndFlush(row);
	}

	private static ServiceRequest toDomain(ServiceRequestEntity e) {
		return new ServiceRequest(e.id, e.code, e.requesterId, e.categoryId, e.locationId, e.description,
				e.priorityId, RequestStatus.valueOf(e.status), e.areaId, e.technicianId,
				e.closureReason == null ? null : ClosureReason.valueOf(e.closureReason), e.solution, e.createdAt,
				e.dueAt, e.updatedAt);
	}

}
