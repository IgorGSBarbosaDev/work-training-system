package dev.igorbarbosa.worktrainingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import dev.igorbarbosa.worktrainingsystem.shared.config.JacksonConfiguration;

@SpringBootApplication
@ConfigurationPropertiesScan
@Import(JacksonConfiguration.class)
public class WorkTrainingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkTrainingSystemApplication.class, args);
	}

}
