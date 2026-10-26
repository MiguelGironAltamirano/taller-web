package pe.edu.unmsm.servicios.workflow.infrastructure.scheduling;

import org.springframework.context.ApplicationContext;

/** Lets tests outside this package check whether the package-private job is registered. */
public final class AutoCloseJobAccess {

	private AutoCloseJobAccess() {
	}

	public static boolean isRegistered(ApplicationContext context) {
		return context.getBeanNamesForType(AutoCloseJob.class).length > 0;
	}

}
