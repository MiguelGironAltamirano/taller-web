package pe.edu.unmsm.servicios;

import org.springframework.boot.SpringApplication;

public class TestServiciosApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServiciosApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
