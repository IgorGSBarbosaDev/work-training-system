package dev.igorbarbosa.worktrainingsystem.qrverification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "employee_qr_codes")
public class EmployeeQrCode {
	public enum Status { ACTIVE, REVOKED }
	@Id private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
	@Column(name = "token_hash", nullable = false, updatable = false, length = 64) private String tokenHash;
	@Column(name = "token_ciphertext", nullable = false, updatable = false, length = 512) private String tokenCiphertext;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;
	@Column(name = "generated_at", nullable = false, updatable = false) private Instant generatedAt;
	@Column(name = "generated_by_user_id", nullable = false, updatable = false) private UUID generatedByUserId;
	@Column(name = "revoked_at") private Instant revokedAt;
	@Column(name = "revoked_by_user_id") private UUID revokedByUserId;
	@Column(name = "revocation_reason", length = 1000) private String revocationReason;
	protected EmployeeQrCode() {}
	public EmployeeQrCode(UUID organizationId, UUID employeeId, String tokenHash, String tokenCiphertext, UUID generator, Instant now) { this.id=UUID.randomUUID(); this.organizationId=organizationId; this.employeeId=employeeId; this.tokenHash=tokenHash; this.tokenCiphertext=tokenCiphertext; this.status=Status.ACTIVE; this.generatedAt=now; this.generatedByUserId=generator; }
	public void revoke(UUID actor, String reason, Instant now) { if (status == Status.REVOKED) return; status=Status.REVOKED; revokedAt=now; revokedByUserId=actor; revocationReason=reason; }
	public UUID getId(){return id;} public UUID getOrganizationId(){return organizationId;} public UUID getEmployeeId(){return employeeId;} public String getTokenHash(){return tokenHash;} public String getTokenCiphertext(){return tokenCiphertext;} public Status getStatus(){return status;} public Instant getGeneratedAt(){return generatedAt;} public UUID getGeneratedByUserId(){return generatedByUserId;} public Instant getRevokedAt(){return revokedAt;} public UUID getRevokedByUserId(){return revokedByUserId;} public String getRevocationReason(){return revocationReason;}
}
