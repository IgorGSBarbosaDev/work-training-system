package dev.igorbarbosa.worktrainingsystem.files.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 32) private FilePurpose purpose;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private FileState state;
	@Column(name = "requested_by_user_id", nullable = false, updatable = false) private UUID requestedByUserId;
	@Column(name = "owner_employee_id", updatable = false) private UUID ownerEmployeeId;
	@Column(name = "original_file_name", nullable = false, updatable = false, length = 255) private String originalFileName;
	@Column(name = "object_key", nullable = false, updatable = false, length = 1024) private String objectKey;
	@Column(name = "expected_content_type", nullable = false, updatable = false, length = 100) private String expectedContentType;
	@Column(name = "expected_size_bytes", nullable = false, updatable = false) private long expectedSizeBytes;
	@Column(name = "expected_checksum_sha256", updatable = false, length = 64) private String expectedChecksumSha256;
	@Column(name = "actual_content_type", length = 100) private String actualContentType;
	@Column(name = "actual_size_bytes") private Long actualSizeBytes;
	@Column(name = "actual_checksum_sha256", length = 64) private String actualChecksumSha256;
	@Column(name = "expires_at", nullable = false, updatable = false) private Instant expiresAt;
	@Column(name = "uploaded_at") private Instant uploadedAt;
	@Column(name = "failed_at") private Instant failedAt;
	@Column(name = "failure_reason", length = 1000) private String failureReason;
	@Column(name = "cancelled_at") private Instant cancelledAt;
	@Column(name = "expired_at") private Instant expiredAt;

	protected UploadedFile() {}

	public UploadedFile(UUID organizationId, FilePurpose purpose, UUID requestedByUserId, UUID ownerEmployeeId,
			String originalFileName, String objectKey, String expectedContentType, long expectedSizeBytes,
			String expectedChecksumSha256, Instant expiresAt) {
		this.organizationId = organizationId;
		this.purpose = purpose;
		this.state = FileState.REQUESTED;
		this.requestedByUserId = requestedByUserId;
		this.ownerEmployeeId = ownerEmployeeId;
		this.originalFileName = originalFileName;
		this.objectKey = objectKey;
		this.expectedContentType = expectedContentType;
		this.expectedSizeBytes = expectedSizeBytes;
		this.expectedChecksumSha256 = expectedChecksumSha256;
		this.expiresAt = expiresAt;
	}

	public void complete(String contentType, long sizeBytes, String checksum, Instant now) {
		requireRequested();
		state = FileState.UPLOADED;
		actualContentType = contentType;
		actualSizeBytes = sizeBytes;
		actualChecksumSha256 = checksum;
		uploadedAt = now;
	}

	public void fail(String reason, Instant now) {
		requireRequested();
		state = FileState.FAILED;
		failedAt = now;
		failureReason = reason;
	}

	public void cancel(Instant now) { requireRequested(); state = FileState.CANCELLED; cancelledAt = now; }
	public void expire(Instant now) { requireRequested(); state = FileState.EXPIRED; expiredAt = now; }
	private void requireRequested() { if (state != FileState.REQUESTED) throw new IllegalStateException("file state"); }

	public UUID getOrganizationId() { return organizationId; }
	public FilePurpose getPurpose() { return purpose; }
	public FileState getState() { return state; }
	public UUID getRequestedByUserId() { return requestedByUserId; }
	public UUID getOwnerEmployeeId() { return ownerEmployeeId; }
	public String getOriginalFileName() { return originalFileName; }
	public String getObjectKey() { return objectKey; }
	public String getExpectedContentType() { return expectedContentType; }
	public long getExpectedSizeBytes() { return expectedSizeBytes; }
	public String getExpectedChecksumSha256() { return expectedChecksumSha256; }
	public String getActualContentType() { return actualContentType; }
	public Long getActualSizeBytes() { return actualSizeBytes; }
	public String getActualChecksumSha256() { return actualChecksumSha256; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getUploadedAt() { return uploadedAt; }
}
