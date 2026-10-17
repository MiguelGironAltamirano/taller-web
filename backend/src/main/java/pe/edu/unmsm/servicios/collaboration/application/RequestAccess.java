package pe.edu.unmsm.servicios.collaboration.application;

import java.util.List;

import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public interface RequestAccess {

	RequestSnapshot findVisibleOrThrow(AuthenticatedUser user, String code);

	List<Long> findSupervisorIds(long areaId);

}
