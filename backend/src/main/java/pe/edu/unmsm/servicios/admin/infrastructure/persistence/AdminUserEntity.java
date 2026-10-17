package pe.edu.unmsm.servicios.admin.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Administration view of the {@code users} table owned by the auth module (same columns as its entity). */
@Entity
@Table(name = "users")
class AdminUserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "full_name", nullable = false, length = 150)
	String fullName;

	@Column(nullable = false, length = 150)
	String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	String passwordHash;

	@Column(nullable = false, length = 20)
	String role;

	@Column(name = "requester_type", length = 20)
	String requesterType;

	@Column(name = "area_id")
	Long areaId;

	@Column(nullable = false)
	boolean active;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

}
