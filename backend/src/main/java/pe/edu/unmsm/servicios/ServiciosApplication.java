package pe.edu.unmsm.servicios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiciosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiciosApplication.class, args);
	}

}
