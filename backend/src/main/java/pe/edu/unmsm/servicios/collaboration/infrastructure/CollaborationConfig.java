package pe.edu.unmsm.servicios.collaboration.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.collaboration.application.CommentNotifier;
import pe.edu.unmsm.servicios.collaboration.application.CommentRepository;
import pe.edu.unmsm.servicios.collaboration.application.CreateComment;
import pe.edu.unmsm.servicios.collaboration.application.ListComments;
import pe.edu.unmsm.servicios.collaboration.application.RequestAccess;

@Configuration
class CollaborationConfig {

	@Bean
	ListComments listComments(RequestAccess requestAccess, CommentRepository commentRepository) {
		return new ListComments(requestAccess, commentRepository);
	}

	@Bean
	CreateComment createComment(RequestAccess requestAccess, CommentRepository commentRepository,
			CommentNotifier commentNotifier) {
		return new CreateComment(requestAccess, commentRepository, commentNotifier);
	}

}
