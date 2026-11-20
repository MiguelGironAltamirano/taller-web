package pe.edu.unmsm.servicios.shared.infrastructure.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Last resort for a bug no handler expected: logs it in full and answers a generic {@code 500} problem in Spanish, so
 * the client never sees a stack trace, a class name or data from the failure. Runs after Spring's own resolvers (and
 * therefore after every module's {@code @ExceptionHandler}), which keeps their more specific answers.
 */
@Component
class UnexpectedErrorResolver implements HandlerExceptionResolver, Ordered {

	private static final Logger log = LoggerFactory.getLogger(UnexpectedErrorResolver.class);

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		if (response.isCommitted()) {
			return null;
		}
		try {
			response.reset();
			response.setStatus(500);
			response.setContentType("application/problem+json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Internal Server Error\",\"status\":500,"
					+ "\"detail\":\"" + GeneralExceptionHandler.UNEXPECTED + "\",\"instance\":\""
					+ request.getRequestURI().replace("\"", "") + "\"}");
			return new ModelAndView();
		}
		catch (IOException writeFailed) {
			return null;
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
