package dev.igorbarbosa.worktrainingsystem.qrverification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "qr_code_access_logs")
public class QrCodeAccessLog {
	@Id private UUID id;
	@Column(name="organization_id", nullable=false, updatable=false) private UUID organizationId;
	@Column(name="qr_code_id", updatable=false) private UUID qrCodeId;
	@Column(name="queried_by_user_id", nullable=false, updatable=false) private UUID queriedByUserId;
	@Column(name="queried_at", nullable=false, updatable=false) private Instant queriedAt;
	@Column(nullable=false, updatable=false, length=24) private String result;
	@Column(name="request_id", nullable=false, updatable=false, length=128) private String requestId;
	@Column(name="token_hash", nullable=false, updatable=false, length=64) private String tokenHash;
	@Column(name="technical_metadata", nullable=false, updatable=false, columnDefinition="jsonb") private String technicalMetadata;
	protected QrCodeAccessLog() {}
	public QrCodeAccessLog(UUID organizationId, UUID qrCodeId, UUID userId, Instant now, String result, String requestId, String tokenHash) { this.id=UUID.randomUUID(); this.organizationId=organizationId; this.qrCodeId=qrCodeId; this.queriedByUserId=userId; this.queriedAt=now; this.result=result; this.requestId=requestId; this.tokenHash=tokenHash; this.technicalMetadata="{}"; }
	public UUID getId(){return id;} public UUID getQrCodeId(){return qrCodeId;} public UUID getQueriedByUserId(){return queriedByUserId;} public Instant getQueriedAt(){return queriedAt;} public String getResult(){return result;} public String getRequestId(){return requestId;}
}
