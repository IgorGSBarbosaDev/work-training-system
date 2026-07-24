package dev.igorbarbosa.worktrainingsystem;

import org.springframework.boot.SpringApplication;

public class TestWorkTrainingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(WorkTrainingSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
