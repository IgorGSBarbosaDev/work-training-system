package dev.igorbarbosa.worktrainingsystem.progress.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort.ExecutionAssignment;
import dev.igorbarbosa.worktrainingsystem.progress.api.LearningPathResponse;
import dev.igorbarbosa.worktrainingsystem.progress.api.ResumePointResponse;
import dev.igorbarbosa.worktrainingsystem.progress.api.VideoProgressRequest;
import dev.igorbarbosa.worktrainingsystem.progress.api.VideoProgressResponse;
import dev.igorbarbosa.worktrainingsystem.progress.config.ProgressProperties;
import dev.igorbarbosa.worktrainingsystem.progress.domain.VideoProgress;
import dev.igorbarbosa.worktrainingsystem.progress.domain.VideoProgressEvent;
import dev.igorbarbosa.worktrainingsystem.progress.persistence.VideoProgressEventRepository;
import dev.igorbarbosa.worktrainingsystem.progress.persistence.VideoProgressRepository;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionContent;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionVideo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoProgressService implements ContentProgressPort {
	private final VideoProgressRepository progress;
	private final VideoProgressEventRepository events;
	private final AssignmentExecutionPort assignments;
	private final TrainingExecutionCatalog trainings;
	private final ProgressProperties properties;
	private final Clock clock;

	public VideoProgressService(VideoProgressRepository progress, VideoProgressEventRepository events,
			AssignmentExecutionPort assignments, TrainingExecutionCatalog trainings,
			ProgressProperties properties, Clock clock) {
		this.progress = progress; this.events = events; this.assignments = assignments;
		this.trainings = trainings; this.properties = properties; this.clock = clock;
	}

	@Transactional
	public VideoProgressResponse update(UUID assignmentId, UUID videoId, VideoProgressRequest request) {
		ExecutionAssignment assignment = assignments.requireOwner(assignmentId, false);
		ExecutionVideo video = trainings.requireVideo(assignment.trainingVersionId(), videoId);
		UUID trainingVersionId = assignment.trainingVersionId();
		String eventId = request.eventId() == null || request.eventId().isBlank()
				? "derived:" + hash(request.positionSeconds(), request.watchedSeconds(), request.reportedPercentage(),
						request.eventAt())
				: request.eventId().trim();
		String requestHash = hash(request.positionSeconds(), request.watchedSeconds(), request.reportedPercentage(),
				request.eventAt(), eventId, request.finalEvent());
		var duplicate = events.findByOrganizationIdAndAssignmentIdAndVideoIdAndEventIdentifier(
				DEFAULT_ORGANIZATION_ID, assignmentId, videoId, eventId);
		if (duplicate.isPresent()) {
			if (!duplicate.get().getRequestHash().equals(requestHash))
				throw conflict("PROGRESS_EVENT_REUSED", "O identificador do evento foi reutilizado com outros dados.");
			return response(duplicate.get());
		}
		assignment = assignments.requireOwner(assignmentId, true);

		VideoProgress item = progress.findByOrganizationIdAndAssignmentIdAndVideoId(
				DEFAULT_ORGANIZATION_ID, assignmentId, videoId)
				.orElseGet(() -> progress.saveAndFlush(new VideoProgress(DEFAULT_ORGANIZATION_ID, assignmentId,
						trainingVersionId, videoId)));
		Instant now = clock.instant();
		BigDecimal requestedWatched = request.watchedSeconds().setScale(3, RoundingMode.DOWN);
		BigDecimal delta = request.eventId() == null || request.eventId().isBlank()
				? requestedWatched.subtract(item.getWatchedSeconds()).max(BigDecimal.ZERO)
				: requestedWatched;
		validate(request, item, video.durationSeconds(), delta, now);
		BigDecimal watchedAfter = item.getWatchedSeconds().add(delta).min(BigDecimal.valueOf(video.durationSeconds()));
		BigDecimal percentage = percentage(watchedAfter, video.durationSeconds());
		if (request.reportedPercentage().compareTo(percentage.add(properties.reportedPercentageTolerance())) > 0)
			throw violation("REPORTED_PROGRESS_IMPLAUSIBLE", "O percentual informado excede o progresso validado.");
		item.accept(request.positionSeconds(), delta, percentage, request.eventAt(), now, requestHash, video.durationSeconds());

		ExecutionContent content = trainings.content(assignment.trainingVersionId());
		Map<UUID, VideoProgress> allProgress = byVideo(progress.findAllByOrganizationIdAndAssignmentId(DEFAULT_ORGANIZATION_ID, assignmentId));
		allProgress.put(videoId, item);
		boolean contentReady = content.activeVideos().stream().filter(ExecutionVideo::required)
				.allMatch(required -> allProgress.containsKey(required.id()) && allProgress.get(required.id()).isCompleted());
		if (contentReady) assignment = assignments.contentReady(assignmentId, content.hasActiveQuestionnaires());
		progress.flush();
		events.save(new VideoProgressEvent(DEFAULT_ORGANIZATION_ID, item, eventId, request.eventAt(), now, requestHash,
				request.positionSeconds(), delta, request.reportedPercentage().setScale(2, RoundingMode.HALF_UP), assignment.status()));
		return response(item, assignment.status());
	}

	@Transactional(readOnly = true)
	public VideoProgressResponse get(UUID assignmentId, UUID videoId) {
		ExecutionAssignment assignment = assignments.view(assignmentId);
		trainings.requireVideo(assignment.trainingVersionId(), videoId);
		return progress.findByOrganizationIdAndAssignmentIdAndVideoId(DEFAULT_ORGANIZATION_ID, assignmentId, videoId)
				.map(item -> response(item, assignment.status()))
				.orElseGet(() -> empty(assignment, videoId));
	}

	@Transactional(readOnly = true)
	public LearningPathResponse learningPath(UUID assignmentId) {
		ExecutionAssignment assignment = assignments.view(assignmentId);
		return learningPath(assignment);
	}

	@Transactional(readOnly = true)
	public LearningPathResponse ownLearningPath(UUID assignmentId) {
		return learningPath(assignments.requireOwner(assignmentId, false));
	}

	@Transactional(readOnly = true)
	public ResumePointResponse resumePoint(UUID assignmentId) {
		ExecutionAssignment assignment = assignments.requireOwner(assignmentId, false);
		ExecutionContent content = trainings.content(assignment.trainingVersionId());
		var latest = progress.findFirstByOrganizationIdAndAssignmentIdOrderByUpdatedAtDesc(DEFAULT_ORGANIZATION_ID, assignmentId);
		if (latest.isPresent()) {
			ExecutionVideo video = trainings.requireVideo(assignment.trainingVersionId(), latest.get().getVideoId());
			return new ResumePointResponse(assignmentId, video.moduleId(), video.id(), latest.get().getPositionSeconds());
		}
		ExecutionVideo first = content.activeVideos().stream().findFirst().orElse(null);
		return first == null ? new ResumePointResponse(assignmentId, null, null, 0)
				: new ResumePointResponse(assignmentId, first.moduleId(), first.id(), 0);
	}

	@Transactional(readOnly = true)
	public long resumePosition(UUID assignmentId, UUID videoId) {
		return progress.findByOrganizationIdAndAssignmentIdAndVideoId(DEFAULT_ORGANIZATION_ID, assignmentId, videoId)
				.map(VideoProgress::getPositionSeconds).orElse(0L);
	}

	@Override @Transactional(readOnly = true)
	public boolean requiredContentReady(UUID assignmentId, UUID trainingVersionId) {
		ExecutionContent content = trainings.content(trainingVersionId);
		Map<UUID, VideoProgress> current = byVideo(progress.findAllByOrganizationIdAndAssignmentId(
				DEFAULT_ORGANIZATION_ID, assignmentId));
		return content.activeVideos().stream().filter(ExecutionVideo::required)
				.allMatch(video -> current.containsKey(video.id()) && current.get(video.id()).isCompleted());
	}

	private LearningPathResponse learningPath(ExecutionAssignment assignment) {
		ExecutionContent content = trainings.content(assignment.trainingVersionId());
		Map<UUID, VideoProgress> current = byVideo(progress.findAllByOrganizationIdAndAssignmentId(DEFAULT_ORGANIZATION_ID, assignment.id()));
		boolean requiredReady = content.activeVideos().stream().filter(ExecutionVideo::required)
				.allMatch(video -> current.containsKey(video.id()) && current.get(video.id()).isCompleted());
		var modules = content.modules().stream().map(module -> {
			var videos = module.videos().stream().map(video -> {
				VideoProgress item = current.get(video.id());
				return new LearningPathResponse.Video(video.id(), video.title(), video.description(), video.order(),
						video.durationSeconds(), video.required(), item == null ? 0 : item.getPositionSeconds(),
						item == null ? BigDecimal.ZERO.setScale(2) : item.getPercentageWatched(), item != null && item.isCompleted());
			}).toList();
			var questionnaire = module.questionnaire() == null ? null : new LearningPathResponse.Questionnaire(
					module.questionnaire().id(), module.questionnaire().title(), requiredReady);
			return new LearningPathResponse.Module(module.id(), module.title(), module.description(), module.order(), videos, questionnaire);
		}).toList();
		boolean hasAssessment = content.hasActiveQuestionnaires();
		return new LearningPathResponse(assignment.id(), assignment.trainingVersionId(), assignment.status(), modules,
				new LearningPathResponse.Assessment(hasAssessment, hasAssessment && requiredReady,
						hasAssessment ? (requiredReady ? "AVAILABLE" : "WAITING_FOR_REQUIRED_VIDEOS") : "NOT_REQUIRED"));
	}

	private void validate(VideoProgressRequest request, VideoProgress item, int durationSeconds, BigDecimal delta, Instant now) {
		if (request.positionSeconds() < 0 || request.positionSeconds() > durationSeconds)
			throw violation("VIDEO_POSITION_INVALID", "A posição informada excede a duração do vídeo.");
		if (delta.signum() < 0)
			throw violation("WATCHED_DELTA_INVALID", "O tempo assistido não pode ser negativo.");
		if (request.reportedPercentage().signum() < 0 || request.reportedPercentage().compareTo(BigDecimal.valueOf(100)) > 0)
			throw violation("REPORTED_PROGRESS_INVALID", "O percentual informado deve estar entre zero e cem.");
		if (request.eventAt().isAfter(now.plus(properties.futureTolerance())))
			throw violation("PROGRESS_EVENT_FUTURE", "O evento de progresso está no futuro.");
		if (request.eventAt().isBefore(now.minus(properties.maxEventAge())))
			throw violation("PROGRESS_EVENT_STALE", "O evento de progresso expirou.");
		if (item.getLastEventAt() != null && !request.eventAt().isAfter(item.getLastEventAt()))
			throw violation("PROGRESS_EVENT_STALE", "O evento é anterior ao último progresso aceito.");
		if (!request.finalEvent() && item.getLastEventReceivedAt() != null
				&& Duration.between(item.getLastEventReceivedAt(), now).compareTo(properties.minimumEventInterval()) < 0)
			throw violation("PROGRESS_EVENT_THROTTLED", "A atualização de progresso foi enviada cedo demais.");
		BigDecimal plausible = properties.watchedToleranceSeconds();
		if (item.getLastEventAt() != null) {
			BigDecimal eventElapsed = seconds(Duration.between(item.getLastEventAt(), request.eventAt()));
			BigDecimal serverElapsed = seconds(Duration.between(item.getLastEventReceivedAt(), now));
			plausible = eventElapsed.min(serverElapsed).max(BigDecimal.ZERO)
					.multiply(properties.maximumPlaybackRate()).add(properties.watchedToleranceSeconds());
		}
		if (delta.compareTo(plausible) > 0)
			throw violation("WATCHED_DELTA_IMPLAUSIBLE", "O tempo assistido informado é incompatível com o tempo decorrido.");
	}

	private BigDecimal percentage(BigDecimal watched, int durationSeconds) {
		return watched.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(durationSeconds), 2, RoundingMode.DOWN)
				.min(new BigDecimal("100.00"));
	}
	private BigDecimal seconds(Duration duration) {
		return BigDecimal.valueOf(duration.toNanos(), 9);
	}
	private Map<UUID, VideoProgress> byVideo(java.util.List<VideoProgress> values) {
		Map<UUID, VideoProgress> result = new HashMap<>(); values.forEach(value -> result.put(value.getVideoId(), value)); return result;
	}
	private VideoProgressResponse response(VideoProgress item, dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus status) {
		return new VideoProgressResponse(item.getAssignmentId(), item.getVideoId(), item.getPositionSeconds(),
				item.getWatchedSeconds(), item.getPercentageWatched(), item.isCompleted(), status, item.getUpdatedAt());
	}
	private VideoProgressResponse response(VideoProgressEvent event) {
		return new VideoProgressResponse(event.getAssignmentId(), event.getVideoId(), event.getResultingPositionSeconds(),
				event.getResultingWatchedSeconds(), event.getResultingPercentage(), event.isResultingCompleted(),
				event.getResultingAssignmentStatus(), event.getReceivedAt());
	}
	private VideoProgressResponse empty(ExecutionAssignment assignment, UUID videoId) {
		return new VideoProgressResponse(assignment.id(), videoId, 0, BigDecimal.ZERO.setScale(3),
				BigDecimal.ZERO.setScale(2), false, assignment.status(), null);
	}
	private String hash(Object... values) {
		try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(java.util.Arrays.deepToString(values).getBytes(StandardCharsets.UTF_8))); }
		catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
	}
	private BusinessRuleViolationException violation(String code, String message) { return new BusinessRuleViolationException(code, message); }
	private ResourceConflictException conflict(String code, String message) { return new ResourceConflictException(code, message); }
}
