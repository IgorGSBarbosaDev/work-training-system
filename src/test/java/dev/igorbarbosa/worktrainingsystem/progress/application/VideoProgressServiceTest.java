package dev.igorbarbosa.worktrainingsystem.progress.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort.ExecutionAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.progress.api.VideoProgressRequest;
import dev.igorbarbosa.worktrainingsystem.progress.config.ProgressProperties;
import dev.igorbarbosa.worktrainingsystem.progress.domain.VideoProgress;
import dev.igorbarbosa.worktrainingsystem.progress.domain.VideoProgressEvent;
import dev.igorbarbosa.worktrainingsystem.progress.persistence.VideoProgressEventRepository;
import dev.igorbarbosa.worktrainingsystem.progress.persistence.VideoProgressRepository;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionContent;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionModule;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionVideo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VideoProgressServiceTest {
	private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
	private final UUID assignmentId = UUID.randomUUID();
	private final UUID versionId = UUID.randomUUID();
	private final UUID videoId = UUID.randomUUID();
	@Mock private VideoProgressRepository progress;
	@Mock private VideoProgressEventRepository events;
	@Mock private AssignmentExecutionPort assignments;
	@Mock private TrainingExecutionCatalog trainings;
	private VideoProgressService service;
	private ExecutionAssignment assignment;
	private ExecutionVideo video;

	@BeforeEach
	void setUp() {
		service = new VideoProgressService(progress, events, assignments, trainings,
				new ProgressProperties(Duration.ofSeconds(10), Duration.ofMinutes(5), Duration.ofSeconds(1), new BigDecimal("2.0"),
						new BigDecimal("2.0"), new BigDecimal("2.0")), Clock.fixed(NOW, ZoneOffset.UTC));
		assignment = new ExecutionAssignment(assignmentId, DEFAULT_ORGANIZATION_ID, UUID.randomUUID(),
				UUID.randomUUID(), versionId, AssignmentStatus.IN_PROGRESS);
		video = new ExecutionVideo(videoId, UUID.randomUUID(), versionId, "Video", null, 1, 100, true,
				UUID.randomUUID(), "private/video.mp4");
		org.mockito.Mockito.lenient().when(assignments.requireOwner(assignmentId, true)).thenReturn(assignment);
		when(assignments.requireOwner(assignmentId, false)).thenReturn(assignment);
		when(trainings.requireVideo(versionId, videoId)).thenReturn(video);
		when(events.findByOrganizationIdAndAssignmentIdAndVideoIdAndEventIdentifier(any(), any(), any(), any()))
				.thenReturn(Optional.empty());
	}

	@Test
	void completesAtExactEightyButNotAtSeventyNinePointNinetyNine() {
		VideoProgress below = baseline();
		stubProgress(below, content(video));
		var response = service.update(assignmentId, videoId, request("79.990", "79.99", NOW, "below", 80));
		assertThat(response.percentageWatched()).isEqualByComparingTo("79.99");
		assertThat(response.completed()).isFalse();
		verify(assignments, never()).contentReady(any(), any(Boolean.class));

		VideoProgress exact = baseline();
		when(progress.findByOrganizationIdAndAssignmentIdAndVideoId(DEFAULT_ORGANIZATION_ID, assignmentId, videoId))
				.thenReturn(Optional.of(exact));
		when(progress.findAllByOrganizationIdAndAssignmentId(DEFAULT_ORGANIZATION_ID, assignmentId)).thenReturn(List.of(exact));
		when(assignments.contentReady(assignmentId, false)).thenReturn(assignment);
		response = service.update(assignmentId, videoId, request("80.000", "80.00", NOW, "exact", 80));
		assertThat(response.completed()).isTrue();
		verify(assignments).contentReady(assignmentId, false);
	}

	@Test
	void openingAndSeekingDoNotAddWatchTimeOrComplete() {
		VideoProgress item = baseline(); stubProgress(item, content(video));
		var response = service.update(assignmentId, videoId, request("0", "0", NOW, "seek", 95));
		assertThat(response.positionSeconds()).isEqualTo(95);
		assertThat(response.watchedSeconds()).isEqualByComparingTo("0");
		assertThat(response.completed()).isFalse();
	}

	@Test
	void neverRegressesAndCapsAccumulatedWatchTime() {
		VideoProgress item = baseline();
		item.accept(90, new BigDecimal("99"), new BigDecimal("99"), NOW.minusSeconds(50), NOW.minusSeconds(50), "a", 100);
		stubProgress(item, content(video));
		when(assignments.contentReady(assignmentId, false)).thenReturn(assignment);
		var response = service.update(assignmentId, videoId, request("5", "5", NOW, "cap", 10));
		assertThat(response.positionSeconds()).isEqualTo(90);
		assertThat(response.watchedSeconds()).isEqualByComparingTo("100.000");
		assertThat(response.percentageWatched()).isEqualByComparingTo("100.00");
	}

	@Test
	void rejectsImplausibleFutureAndStaleEvents() {
		VideoProgress item = baseline(); stubProgress(item, content(video));
		assertCode("WATCHED_DELTA_IMPLAUSIBLE", request("300", "100", NOW, "fast", 1));
		assertCode("PROGRESS_EVENT_FUTURE", request("0", "0", NOW.plusSeconds(11), "future", 1));
		assertCode("PROGRESS_EVENT_STALE", request("0", "0", NOW.minus(Duration.ofMinutes(6)), "stale", 1));
	}

	@Test
	void duplicateReturnsRecordedResultWithoutAddingTime() {
		VideoProgressEvent event = org.mockito.Mockito.mock(VideoProgressEvent.class);
		when(event.getRequestHash()).thenReturn(hashFor(request("0", "0", NOW, "same", 0)));
		when(event.getAssignmentId()).thenReturn(assignmentId); when(event.getVideoId()).thenReturn(videoId);
		when(event.getResultingWatchedSeconds()).thenReturn(new BigDecimal("20.000"));
		when(event.getResultingPercentage()).thenReturn(new BigDecimal("20.00"));
		when(event.getResultingAssignmentStatus()).thenReturn(AssignmentStatus.IN_PROGRESS);
		when(event.getReceivedAt()).thenReturn(NOW);
		when(events.findByOrganizationIdAndAssignmentIdAndVideoIdAndEventIdentifier(DEFAULT_ORGANIZATION_ID,
				assignmentId, videoId, "same")).thenReturn(Optional.of(event));
		var response = service.update(assignmentId, videoId, request("0", "0", NOW, "same", 0));
		assertThat(response.watchedSeconds()).isEqualByComparingTo("20");
		verify(progress, never()).save(any());
	}

	@Test
	void optionalVideoDoesNotBlockReadinessAndVersionIsExact() {
		ExecutionVideo optional = new ExecutionVideo(UUID.randomUUID(), video.moduleId(), versionId, "Optional", null,
				2, 100, false, UUID.randomUUID(), "private/optional.mp4");
		VideoProgress item = baseline(); stubProgress(item, content(video, optional));
		when(assignments.contentReady(assignmentId, false)).thenReturn(assignment);
		service.update(assignmentId, videoId, request("80", "80", NOW, "ready", 80));
		verify(trainings).requireVideo(versionId, videoId);
		verify(assignments).contentReady(assignmentId, false);
	}

	private VideoProgress baseline() {
		VideoProgress item = new VideoProgress(DEFAULT_ORGANIZATION_ID, assignmentId, versionId, videoId);
		ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
		item.accept(0, BigDecimal.ZERO, BigDecimal.ZERO.setScale(2), NOW.minusSeconds(100), NOW.minusSeconds(100), "baseline", 100);
		return item;
	}
	private void stubProgress(VideoProgress item, ExecutionContent content) {
		when(progress.findByOrganizationIdAndAssignmentIdAndVideoId(DEFAULT_ORGANIZATION_ID, assignmentId, videoId))
				.thenReturn(Optional.of(item));
		org.mockito.Mockito.lenient().when(progress.findAllByOrganizationIdAndAssignmentId(DEFAULT_ORGANIZATION_ID, assignmentId))
				.thenReturn(List.of(item));
		org.mockito.Mockito.lenient().when(trainings.content(versionId)).thenReturn(content);
	}
	private ExecutionContent content(ExecutionVideo... videos) {
		return new ExecutionContent(versionId, List.of(new ExecutionModule(video.moduleId(), "Module", null, 1,
				List.of(videos), null)));
	}
	private VideoProgressRequest request(String watched, String reported, Instant at, String id, long position) {
		return new VideoProgressRequest(position, new BigDecimal(watched), new BigDecimal(reported), at, id, false);
	}
	private void assertCode(String code, VideoProgressRequest request) {
		assertThatThrownBy(() -> service.update(assignmentId, videoId, request))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo(code);
	}
	private String hashFor(VideoProgressRequest request) {
		try {
			Object[] values = {request.positionSeconds(), request.watchedSeconds(), request.reportedPercentage(), request.eventAt(),
					request.eventId(), request.finalEvent()};
			return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
					.digest(java.util.Arrays.deepToString(values).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		} catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
	}
}
