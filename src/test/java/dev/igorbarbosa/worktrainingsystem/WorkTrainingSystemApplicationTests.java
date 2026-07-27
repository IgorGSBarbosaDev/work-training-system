package dev.igorbarbosa.worktrainingsystem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class WorkTrainingSystemApplicationTests {

	private final Flyway flyway;

	@Autowired
	WorkTrainingSystemApplicationTests(Flyway flyway) {
		this.flyway = flyway;
	}

	@Test
	void contextLoadsAndRunsMigrations() {
		MigrationInfo migration = flyway.info().current();

		assertThat(migration).isNotNull();
		assertThat(migration.getVersion().getVersion()).isEqualTo("12");
		assertThat(migration.getDescription()).isEqualTo("phase 5 slice b notifications and audit");
	}

}
