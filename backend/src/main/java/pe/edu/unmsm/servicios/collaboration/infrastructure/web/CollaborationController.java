package pe.edu.unmsm.servicios.collaboration.infrastructure.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.collaboration.application.CreateComment;
import pe.edu.unmsm.servicios.collaboration.application.ListComments;
import pe.edu.unmsm.servicios.collaboration.domain.Comment;
import pe.edu.unmsm.servicios.collaboration.infrastructure.web.CollaborationDtos.CommentResponse;
import pe.edu.unmsm.servicios.collaboration.infrastructure.web.CollaborationDtos.CreateCommentRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/requests/{code}/comments")
class CollaborationController {

	private final ListComments listComments;
	private final CreateComment createComment;

	CollaborationController(ListComments listComments, CreateComment createComment) {
		this.listComments = listComments;
		this.createComment = createComment;
	}

	@GetMapping
	List<CommentResponse> list(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		return listComments.execute(user, code).stream().map(CommentResponse::from).toList();
	}

	@PostMapping
	ResponseEntity<CommentResponse> create(@PathVariable String code,
			@Valid @RequestBody CreateCommentRequest request, @AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		Comment comment = createComment.execute(user, code, request.text());
		return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(comment));
	}

}
