package pe.edu.unmsm.servicios.collaboration.application;

import java.util.List;

public interface CommentNotifier {

	void notifyComment(String requestCode, long requestId, List<Long> recipientIds, String text);

}
