package pe.edu.unmsm.servicios.collaboration.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.collaboration.domain.Comment;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class CreateComment {

	private final RequestAccess requestAccess;
	private final CommentRepository commentRepository;
	private final CommentNotifier commentNotifier;

	public CreateComment(RequestAccess requestAccess, CommentRepository commentRepository,
			CommentNotifier commentNotifier) {
		this.requestAccess = requestAccess;
		this.commentRepository = commentRepository;
		this.commentNotifier = commentNotifier;
	}

	@Transactional
	public Comment execute(AuthenticatedUser user, String code, String text) {
		RequestSnapshot request = requestAccess.findVisibleOrThrow(user, code);
		Comment comment = Comment.create(request.id(), user.id(), user.fullName(), text, Instant.now());
		Comment saved = commentRepository.save(comment);

		List<Long> recipients = new ArrayList<>();
		if (request.requesterId() != user.id()) {
			recipients.add(request.requesterId());
		}
		if (request.technicianId() != null) {
			if (request.technicianId() != user.id()) {
				recipients.add(request.technicianId());
			}
		}
		else {
			for (Long supId : requestAccess.findSupervisorIds(request.areaId())) {
				if (supId != user.id() && !recipients.contains(supId)) {
					recipients.add(supId);
				}
			}
		}

		if (!recipients.isEmpty() && commentNotifier != null) {
			String notificationText = user.fullName() + " comentó en la solicitud " + code + ".";
			commentNotifier.notifyComment(code, request.id(), recipients, notificationText);
		}

		return saved;
	}

}
