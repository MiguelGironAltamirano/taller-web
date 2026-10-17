package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "service_requests")
class ServiceRequestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(nullable = false, length = 20)
	String code;

	@Column(name = "requester_id", nullable = false)
	long requesterId;

	@Column(name = "category_id", nullable = false)
	long categoryId;

	@Column(name = "location_id", nullable = false)
	long locationId;

	@Column(nullable = false, length = 2000)
	String description;

	@Column(name = "priority_id", nullable = false)
	long priorityId;

	@Column(nullable = false, length = 20)
	String status;

	@Column(name = "area_id", nullable = false)
	long areaId;

	@Column(name = "technician_id")
	Long technicianId;

	@Column(name = "closure_reason", length = 20)
	String closureReason;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

	@Column(name = "due_at", nullable = false)
	Instant dueAt;

	@Column(name = "updated_at", nullable = false)
	Instant updatedAt;

	/** Optimistic locking: concurrent state changes of the same request are detected, not overwritten. */
	@Version
	@Column(nullable = false)
	long version;

}
