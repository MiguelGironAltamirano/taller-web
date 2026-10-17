package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "request_history")
class RequestHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "request_id", nullable = false)
	long requestId;

	@Column(name = "from_status", length = 20)
	String fromStatus;

	@Column(name = "to_status", nullable = false, length = 20)
	String toStatus;

	@Column(name = "actor_id", nullable = false)
	long actorId;

	@Column(name = "technician_id")
	Long technicianId;

	@Column(length = 500)
	String reason;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

}
