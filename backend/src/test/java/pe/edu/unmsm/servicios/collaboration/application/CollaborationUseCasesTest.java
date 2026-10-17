package pe.edu.unmsm.servicios.collaboration.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.collaboration.domain.Comment;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollaborationUseCasesTest {

	private FakeRequestAccess requestAccess;
	private InMemoryCommentRepository commentRepository;
	private FakeCommentNotifier notifier;
	private ListComments listComments;
	private CreateComment createComment;

	private final AuthenticatedUser requester = new AuthenticatedUser(100L, "req@unmsm.edu.pe", "Ana Solicitante",
			"SOLICITANTE", null);
	private final AuthenticatedUser stranger = new AuthenticatedUser(999L, "stranger@unmsm.edu.pe", "Extraño",
			"SOLICITANTE", null);

	@BeforeEach
	void setUp() {
		requestAccess = new FakeRequestAccess();
		commentRepository = new InMemoryCommentRepository();
		notifier = new FakeCommentNotifier();
		listComments = new ListComments(requestAccess, commentRepository);
		createComment = new CreateComment(requestAccess, commentRepository, notifier);

		RequestSnapshot snapshot = new RequestSnapshot(1L, "SOL-2026-0001", 100L, 5L, 200L);
		requestAccess.requests.add(snapshot);
	}

	@Test
	void listCommentsForVisibleRequestReturnsChronologicalComments() {
		commentRepository.save(new Comment(1L, 1L, 100L, "Ana Solicitante", "Primer comentario", Instant.parse("2026-10-17T10:00:00Z")));
		commentRepository.save(new Comment(2L, 1L, 200L, "Carlos Técnico", "Segundo comentario", Instant.parse("2026-10-17T11:00:00Z")));

		List<Comment> result = listComments.execute(requester, "SOL-2026-0001");

		assertThat(result).hasSize(2);
		assertThat(result.get(0).text()).isEqualTo("Primer comentario");
		assertThat(result.get(1).text()).isEqualTo("Segundo comentario");
	}

	@Test
	void listCommentsForInvisibleRequestThrowsRequestNotFoundException() {
		assertThatThrownBy(() -> listComments.execute(stranger, "SOL-2026-0001"))
				.isInstanceOf(RequestNotFoundException.class);
	}

	@Test
	void createCommentForVisibleRequestSavesAndNotifiesOtherParticipants() {
		Comment created = createComment.execute(requester, "SOL-2026-0001", "Necesito apoyo urgente.");

		assertThat(created.id()).isNotNull();
		assertThat(created.text()).isEqualTo("Necesito apoyo urgente.");
		assertThat(created.authorId()).isEqualTo(100L);
		assertThat(created.authorName()).isEqualTo("Ana Solicitante");

		List<Comment> all = commentRepository.findByRequestId(1L);
		assertThat(all).hasSize(1);
		assertThat(all.get(0).text()).isEqualTo("Necesito apoyo urgente.");

		// Notificó a los otros participantes (el técnico 200L, excluyendo a Ana 100L)
		assertThat(notifier.notificationsSent).hasSize(1);
		FakeCommentNotifier.NotificationRecord note = notifier.notificationsSent.get(0);
		assertThat(note.requestCode()).isEqualTo("SOL-2026-0001");
		assertThat(note.recipientIds()).containsExactly(200L);
	}

	@Test
	void createCommentForInvisibleRequestThrowsRequestNotFoundException() {
		assertThatThrownBy(() -> createComment.execute(stranger, "SOL-2026-0001", "Texto no permitido."))
				.isInstanceOf(RequestNotFoundException.class);
		assertThat(commentRepository.comments).isEmpty();
		assertThat(notifier.notificationsSent).isEmpty();
	}

	private static class FakeRequestAccess implements RequestAccess {
		final List<RequestSnapshot> requests = new ArrayList<>();

		@Override
		public RequestSnapshot findVisibleOrThrow(AuthenticatedUser user, String code) {
			RequestSnapshot found = requests.stream().filter(r -> r.code().equals(code)).findFirst()
					.orElseThrow(() -> new RequestNotFoundException(code));
			// Simula regla de visibilidad: requester, supervisor de área, técnico asignado, admin
			boolean visible = user.role().equals("ADMINISTRADOR")
					|| (user.role().equals("SOLICITANTE") && user.id() == found.requesterId())
					|| (user.role().equals("SUPERVISOR") && user.areaId() != null && user.areaId() == found.areaId())
					|| (user.role().equals("TECNICO") && found.technicianId() != null && user.id() == found.technicianId());
			if (!visible) {
				throw new RequestNotFoundException(code);
			}
			return found;
		}

		@Override
		public List<Long> findSupervisorIds(long areaId) {
			return List.of(300L);
		}
	}

	private static class InMemoryCommentRepository implements CommentRepository {
		final List<Comment> comments = new ArrayList<>();
		long seq = 0;

		@Override
		public Comment save(Comment comment) {
			Comment withId = new Comment(++seq, comment.requestId(), comment.authorId(), comment.authorName(),
					comment.text(), comment.createdAt());
			comments.add(withId);
			return withId;
		}

		@Override
		public List<Comment> findByRequestId(long requestId) {
			return comments.stream().filter(c -> c.requestId() == requestId).toList();
		}
	}

	private static class FakeCommentNotifier implements CommentNotifier {
		record NotificationRecord(String requestCode, long requestId, List<Long> recipientIds, String text) {}
		final List<NotificationRecord> notificationsSent = new ArrayList<>();

		@Override
		public void notifyComment(String requestCode, long requestId, List<Long> recipientIds, String text) {
			notificationsSent.add(new NotificationRecord(requestCode, requestId, recipientIds, text));
		}
	}

}
