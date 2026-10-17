package pe.edu.unmsm.servicios.collaboration.infrastructure.adapter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.collaboration.application.CommentNotifier;

@Component
public class DefaultCommentNotifier implements CommentNotifier {

	private static final Logger log = LoggerFactory.getLogger(DefaultCommentNotifier.class);

	@Override
	public void notifyComment(String requestCode, long requestId, List<Long> recipientIds, String text) {
		log.info("Notifying comment on {} (id: {}) to {}: {}", requestCode, requestId, recipientIds, text);
	}

}
