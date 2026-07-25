package dev.igorbarbosa.worktrainingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorkTrainingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkTrainingSystemApplication.class, args);
	}

}
