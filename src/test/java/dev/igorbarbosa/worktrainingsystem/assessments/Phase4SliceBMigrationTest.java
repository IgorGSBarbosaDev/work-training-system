package dev.igorbarbosa.worktrainingsystem.assessments;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class Phase4SliceBMigrationTest {
	@Test
	void v10DeclaresImmutableAttemptAnswerCompletionAndExpirationHistory() throws Exception {
		try (var stream = getClass().getResourceAsStream("/db/migration/V10__phase_4_slice_b_assessments_and_completions.sql")) {
			String migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(migration).contains("trg_assessment_attempts_immutable", "trg_attempt_answers_immutable",
					"trg_training_completions_immutable", "trg_completion_expiration_history_immutable",
					"uk_training_completions_automatic_assignment", "uk_assessment_attempts_idempotency");
		}
	}
}
