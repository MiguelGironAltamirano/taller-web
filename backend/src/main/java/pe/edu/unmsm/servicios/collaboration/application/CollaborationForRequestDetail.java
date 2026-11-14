package pe.edu.unmsm.servicios.collaboration.application;

import java.util.List;

import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.requests.application.CommentView;
import pe.edu.unmsm.servicios.requests.application.EvidenceView;
import pe.edu.unmsm.servicios.requests.application.RequestCollaboration;

/**
 * Provides the comments and evidences of the request detail through this module's own repositories. It is
 * registered by component scanning so the module keeps its single dependency direction (collaboration →
 * requests); it can move to {@code CollaborationConfig} as an explicit bean without changing anything else.
 */
@Component
public class CollaborationForRequestDetail implements RequestCollaboration {

	private final CommentRepository comments;
	private final EvidenceRepository evidences;

	public CollaborationForRequestDetail(CommentRepository comments, EvidenceRepository evidences) {
		this.comments = comments;
		this.evidences = evidences;
	}

	@Override
	public List<CommentView> comments(long requestId) {
		return comments.findByRequestId(requestId).stream()
				.map(c -> new CommentView(c.id(), c.authorName(), c.text(), c.createdAt()))
				.toList();
	}

	@Override
	public List<EvidenceView> evidences(long requestId) {
		return evidences.findByRequestId(requestId).stream()
				.map(e -> new EvidenceView(e.id(), e.fileName(), e.kind().name(), e.sizeBytes()))
				.toList();
	}

}
