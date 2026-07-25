package dev.igorbarbosa.worktrainingsystem.progress.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_progress")
public class VideoProgress extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "assignment_id", nullable = false, updatable = false) private UUID assignmentId;
	@Column(name = "training_version_id", nullable = false, updatable = false) private UUID trainingVersionId;
	@Column(name = "video_id", nullable = false, updatable = false) private UUID videoId;
	@Column(name = "position_seconds", nullable = false) private long positionSeconds;
	@Column(name = "watched_seconds", nullable = false, precision = 14, scale = 3) private BigDecimal watchedSeconds;
	@Column(name = "percentage_watched", nullable = false, precision = 5, scale = 2) private BigDecimal percentageWatched;
	@Column(nullable = false) private boolean completed;
	@Column(name = "completed_at") private Instant completedAt;
	@Column(name = "last_event_at") private Instant lastEventAt;
	@Column(name = "last_event_received_at") private Instant lastEventReceivedAt;
	@Column(name = "last_event_sequence", nullable = false) private long lastEventSequence;
	@Column(name = "last_event_hash", length = 64) private String lastEventHash;

	protected VideoProgress() {}
	public VideoProgress(UUID organizationId, UUID assignmentId, UUID trainingVersionId, UUID videoId) {
		this.organizationId = organizationId; this.assignmentId = assignmentId; this.trainingVersionId = trainingVersionId;
		this.videoId = videoId; this.watchedSeconds = BigDecimal.ZERO.setScale(3);
		this.percentageWatched = BigDecimal.ZERO.setScale(2);
	}

	public void accept(long requestedPosition, BigDecimal watchedDelta, BigDecimal resultingPercentage,
			Instant eventAt, Instant receivedAt, String eventHash, int durationSeconds) {
		positionSeconds = Math.max(positionSeconds, requestedPosition);
		watchedSeconds = watchedSeconds.add(watchedDelta).min(BigDecimal.valueOf(durationSeconds)).setScale(3);
		percentageWatched = percentageWatched.max(resultingPercentage).setScale(2);
		if (!completed && percentageWatched.compareTo(new BigDecimal("80.00")) >= 0) {
			completed = true; completedAt = receivedAt;
		}
		lastEventAt = eventAt; lastEventReceivedAt = receivedAt; lastEventSequence++; lastEventHash = eventHash;
	}

	public UUID getOrganizationId() { return organizationId; }
	public UUID getAssignmentId() { return assignmentId; }
	public UUID getTrainingVersionId() { return trainingVersionId; }
	public UUID getVideoId() { return videoId; }
	public long getPositionSeconds() { return positionSeconds; }
	public BigDecimal getWatchedSeconds() { return watchedSeconds; }
	public BigDecimal getPercentageWatched() { return percentageWatched; }
	public boolean isCompleted() { return completed; }
	public Instant getCompletedAt() { return completedAt; }
	public Instant getLastEventAt() { return lastEventAt; }
	public Instant getLastEventReceivedAt() { return lastEventReceivedAt; }
	public long getLastEventSequence() { return lastEventSequence; }
	public String getLastEventHash() { return lastEventHash; }
}
