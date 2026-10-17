package pe.edu.unmsm.servicios.collaboration.application;

import java.util.List;

import pe.edu.unmsm.servicios.collaboration.domain.Comment;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class ListComments {

	private final RequestAccess requestAccess;
	private final CommentRepository commentRepository;

	public ListComments(RequestAccess requestAccess, CommentRepository commentRepository) {
		this.requestAccess = requestAccess;
		this.commentRepository = commentRepository;
	}

	public List<Comment> execute(AuthenticatedUser user, String code) {
		RequestSnapshot request = requestAccess.findVisibleOrThrow(user, code);
		return commentRepository.findByRequestId(request.id());
	}

}
