package pe.edu.unmsm.servicios.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "areas")
class AdminAreaEntity {

	@Id
	Long id;

	@Column(nullable = false, length = 100)
	String name;

	@Column(nullable = false)
	boolean active;

}
