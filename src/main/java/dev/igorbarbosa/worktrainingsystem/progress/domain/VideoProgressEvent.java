package dev.igorbarbosa.worktrainingsystem.progress.domain;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_progress_events")
public class VideoProgressEvent {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "progress_id", nullable = false, updatable = false) private UUID progressId;
	@Column(name = "assignment_id", nullable = false, updatable = false) private UUID assignmentId;
	@Column(name = "video_id", nullable = false, updatable = false) private UUID videoId;
	@Column(name = "event_identifier", nullable = false, updatable = false, length = 200) private String eventIdentifier;
	@Column(name = "event_sequence", nullable = false, updatable = false) private long eventSequence;
	@Column(name = "event_at", nullable = false, updatable = false) private Instant eventAt;
	@Column(name = "received_at", nullable = false, updatable = false) private Instant receivedAt;
	@Column(name = "request_hash", nullable = false, updatable = false, length = 64) private String requestHash;
	@Column(name = "requested_position_seconds", nullable = false, updatable = false) private long requestedPositionSeconds;
	@Column(name = "requested_watched_seconds", nullable = false, updatable = false, precision = 14, scale = 3) private BigDecimal requestedWatchedSeconds;
	@Column(name = "reported_percentage", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal reportedPercentage;
	@Column(name = "resulting_position_seconds", nullable = false, updatable = false) private long resultingPositionSeconds;
	@Column(name = "resulting_watched_seconds", nullable = false, updatable = false, precision = 14, scale = 3) private BigDecimal resultingWatchedSeconds;
	@Column(name = "resulting_percentage", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal resultingPercentage;
	@Column(name = "resulting_completed", nullable = false, updatable = false) private boolean resultingCompleted;
	@Enumerated(EnumType.STRING) @Column(name = "resulting_assignment_status", nullable = false, updatable = false, length = 32)
	private AssignmentStatus resultingAssignmentStatus;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected VideoProgressEvent() {}
	public VideoProgressEvent(UUID organizationId, VideoProgress progress, String identifier, Instant eventAt, Instant receivedAt,
			String requestHash, long requestedPosition, BigDecimal requestedWatched, BigDecimal reportedPercentage,
			AssignmentStatus assignmentStatus) {
		this.organizationId = organizationId; this.progressId = progress.getId(); this.assignmentId = progress.getAssignmentId();
		this.videoId = progress.getVideoId(); this.eventIdentifier = identifier; this.eventSequence = progress.getLastEventSequence();
		this.eventAt = eventAt; this.receivedAt = receivedAt; this.requestHash = requestHash;
		this.requestedPositionSeconds = requestedPosition; this.requestedWatchedSeconds = requestedWatched;
		this.reportedPercentage = reportedPercentage; this.resultingPositionSeconds = progress.getPositionSeconds();
		this.resultingWatchedSeconds = progress.getWatchedSeconds(); this.resultingPercentage = progress.getPercentageWatched();
		this.resultingCompleted = progress.isCompleted(); this.resultingAssignmentStatus = assignmentStatus; this.createdAt = receivedAt;
	}
	public String getRequestHash() { return requestHash; }
	public UUID getAssignmentId() { return assignmentId; }
	public UUID getVideoId() { return videoId; }
	public long getResultingPositionSeconds() { return resultingPositionSeconds; }
	public BigDecimal getResultingWatchedSeconds() { return resultingWatchedSeconds; }
	public BigDecimal getResultingPercentage() { return resultingPercentage; }
	public boolean isResultingCompleted() { return resultingCompleted; }
	public AssignmentStatus getResultingAssignmentStatus() { return resultingAssignmentStatus; }
	public Instant getReceivedAt() { return receivedAt; }
}
