package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.RequestRepository;
import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
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
		entity.priorityId = request.priorityId();
		entity.status = request.status().name();
		entity.areaId = request.areaId();
		entity.technicianId = request.technicianId();
		entity.closureReason = request.closureReason() == null ? null : request.closureReason().name();
		entity.createdAt = request.createdAt();
		entity.dueAt = request.dueAt();
		entity.updatedAt = request.updatedAt();
		ServiceRequestEntity stored = requests.saveAndFlush(entity);

		RequestHistoryEntity entry = new RequestHistoryEntity();
		entry.requestId = stored.id;
		entry.fromStatus = firstEntry.fromStatus() == null ? null : firstEntry.fromStatus().name();
		entry.toStatus = firstEntry.toStatus().name();
		entry.actorId = firstEntry.actorId();
		entry.technicianId = firstEntry.technicianId();
		entry.reason = firstEntry.reason();
		entry.createdAt = firstEntry.createdAt();
		history.saveAndFlush(entry);

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
		entity.status = request.status().name();
		entity.technicianId = request.technicianId();
		entity.closureReason = request.closureReason() == null ? null : request.closureReason().name();
		entity.updatedAt = request.updatedAt();
		ServiceRequestEntity stored = requests.saveAndFlush(entity);

		RequestHistoryEntity row = new RequestHistoryEntity();
		row.requestId = stored.id;
		row.fromStatus = entry.fromStatus() == null ? null : entry.fromStatus().name();
		row.toStatus = entry.toStatus().name();
		row.actorId = entry.actorId();
		row.technicianId = entry.technicianId();
		row.reason = entry.reason();
		row.createdAt = entry.createdAt();
		history.saveAndFlush(row);
		return toDomain(stored);
	}

	private static ServiceRequest toDomain(ServiceRequestEntity e) {
		return new ServiceRequest(e.id, e.code, e.requesterId, e.categoryId, e.locationId, e.description,
				e.priorityId, RequestStatus.valueOf(e.status), e.areaId, e.technicianId,
				e.closureReason == null ? null : ClosureReason.valueOf(e.closureReason), e.createdAt, e.dueAt,
				e.updatedAt);
	}

}
