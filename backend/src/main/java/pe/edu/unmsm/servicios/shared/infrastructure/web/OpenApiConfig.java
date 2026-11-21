package pe.edu.unmsm.servicios.shared.infrastructure.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Describes the API in the OpenAPI document Swagger UI reads (/v3/api-docs, /swagger-ui.html; see
 * docs/api/panorama-api.md). The Bearer JWT scheme makes «Authorize» accept the access token returned by
 * POST /api/v1/auth/login; public routes (login, register, system info) simply ignore it. The relative server keeps
 * «Try it out» on the origin Swagger was loaded from, so it works behind nginx, the Compose port and an HTTPS proxy
 * without trusting forwarded headers. Production turns both off with APP_API_DOCS_ENABLED=false
 * (application.properties).
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

	static final String BEARER = "bearerAuth";

	@Bean
	OpenAPI servicesOpenApi(@Value("${app.version}") String version) {
		return new OpenAPI()
				.info(new Info().title("API de servicios universitarios")
						.version(version)
						.description("Gestión de solicitudes de servicios universitarios (UNMSM). Contrato en "
								+ "docs/api/contrato-v1.md; errores en formato RFC 7807. Para llamar rutas "
								+ "protegidas: POST /api/v1/auth/login, copiar accessToken y pegarlo en Authorize."))
				.servers(List.of(new Server().url("/").description("Mismo origen que Swagger UI")))
				.components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
						.description("Token de acceso (15 min) devuelto por POST /api/v1/auth/login")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}

}
