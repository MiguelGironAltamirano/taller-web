package pe.edu.unmsm.servicios.collaboration.application;

import java.util.List;

import pe.edu.unmsm.servicios.collaboration.domain.Comment;

public interface CommentRepository {

	Comment save(Comment comment);

	List<Comment> findByRequestId(long requestId);

}
