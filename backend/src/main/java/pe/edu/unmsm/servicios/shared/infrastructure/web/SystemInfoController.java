package pe.edu.unmsm.servicios.shared.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
class SystemInfoController {

	private final SystemInfoResponse info;

	SystemInfoController(@Value("${spring.application.name}") String name,
			@Value("${app.version}") String version) {
		this.info = new SystemInfoResponse(name, version);
	}

	@GetMapping("/info")
	SystemInfoResponse info() {
		return info;
	}

}
