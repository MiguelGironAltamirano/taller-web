package pe.edu.unmsm.servicios.shared.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	/** Shared with the refresh endpoint, so the client shows the same message whichever call notices it first. */
	static final String SESSION_REPLACED_DETAIL = "Tu sesión se cerró porque ingresaste desde otro dispositivo";

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		if (wasReplaced(ex)) {
			write(response, 401, "Unauthorized", SESSION_REPLACED_DETAIL, "SESSION_REPLACED");
			return;
		}
		write(response, 401, "Unauthorized", "Tu sesión no es válida o expiró. Vuelve a iniciar sesión", null);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException {
		write(response, 403, "Forbidden", "No tienes permiso para realizar esta acción", null);
	}

	private static boolean wasReplaced(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof JwtValidationException invalid) {
				return invalid.getErrors().stream()
						.anyMatch(error -> OpenSessionValidator.SESSION_REPLACED.equals(error.getErrorCode()));
			}
		}
		return false;
	}

	private void write(HttpServletResponse response, int status, String title, String detail, String code)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/problem+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
				+ ",\"detail\":\"" + detail + "\"" + (code == null ? "" : ",\"code\":\"" + code + "\"") + "}");
	}

}
