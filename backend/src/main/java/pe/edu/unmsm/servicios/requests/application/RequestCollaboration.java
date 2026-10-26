package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

/**
 * Comments and evidences of a request, shown in its detail. The collaboration module owns them and provides the
 * implementation, so the dependency goes from collaboration to requests and never the other way around.
 * Callers must have checked that the user may see the request.
 */
public interface RequestCollaboration {

	/** Oldest first. */
	List<CommentView> comments(long requestId);

	/** In upload order. */
	List<EvidenceView> evidences(long requestId);

}
