package dev.igorbarbosa.worktrainingsystem.qrverification.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuditPort;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.qrverification.api.QrAccessLogResponse;
import dev.igorbarbosa.worktrainingsystem.qrverification.api.QrCodeResponse;
import dev.igorbarbosa.worktrainingsystem.qrverification.api.QrVerificationResponse;
import dev.igorbarbosa.worktrainingsystem.qrverification.domain.EmployeeQrCode;
import dev.igorbarbosa.worktrainingsystem.qrverification.domain.QrCodeAccessLog;
import dev.igorbarbosa.worktrainingsystem.qrverification.persistence.EmployeeQrCodeRepository;
import dev.igorbarbosa.worktrainingsystem.qrverification.persistence.QrCodeAccessLogRepository;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationService;
import dev.igorbarbosa.worktrainingsystem.shared.config.JwtProperties;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QrVerificationService {
	private final EmployeeQrCodeRepository codes; private final QrCodeAccessLogRepository logs;
	private final TrainingCompletionRepository completions; private final EmployeeActivityCatalog employees;
	private final TrainingCatalog trainings; private final AuthorizationService authorization; private final CurrentUserProvider currentUser;
	private final QualificationService qualifications;
	private final AuditPort audit; private final JwtProperties jwt; private final QrVerificationProperties qrProperties;
	private final Clock clock; private final SecureRandom random = new SecureRandom();
	public QrVerificationService(EmployeeQrCodeRepository codes, QrCodeAccessLogRepository logs, TrainingCompletionRepository completions,
			EmployeeActivityCatalog employees, TrainingCatalog trainings, AuthorizationService authorization, CurrentUserProvider currentUser,
			QualificationService qualifications, AuditPort audit, JwtProperties jwt, QrVerificationProperties qrProperties,
			Clock clock) { this.codes=codes; this.logs=logs; this.completions=completions; this.employees=employees; this.trainings=trainings; this.authorization=authorization; this.currentUser=currentUser; this.qualifications=qualifications; this.audit=audit; this.jwt=jwt; this.qrProperties=qrProperties; this.clock=clock; }
	@Transactional(readOnly = true) public QrCodeResponse get(UUID employeeId) { requireEmployeeAccess(employeeId); EmployeeQrCode c=codes.findByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID, employeeId, EmployeeQrCode.Status.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("O colaborador não possui QR Code ativo.")); return response(c, decrypt(c.getTokenCiphertext())); }
	@Transactional(readOnly = true) public QrCodeResponse getMine() { UUID employeeId=currentUser.requireCurrentUser().employeeId(); if(employeeId==null)throw new AccessDeniedException("Usuário não está vinculado a colaborador."); return get(employeeId); }
	@Transactional public QrCodeResponse generate(UUID employeeId) { requireAdmin(); EmployeeQrCode active=codes.findByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID, employeeId, EmployeeQrCode.Status.ACTIVE).orElse(null); if (active != null) { active.revoke(currentUser.requireCurrentUser().userId(), "substituído", clock.instant()); codes.flush(); } String token=randomToken(); Instant now=clock.instant(); EmployeeQrCode c=codes.save(new EmployeeQrCode(DEFAULT_ORGANIZATION_ID, employeeId, hash(token), encrypt(token), currentUser.requireCurrentUser().userId(), now)); audit.record(new AuditPort.AuditRecord(currentUser.requireCurrentUser().userId(), "QR_CODE_GENERATED", "EMPLOYEE_QR_CODE", c.getId(), now, java.util.Map.of("employeeId", employeeId.toString()))); return response(c, token); }
	@Transactional public QrCodeResponse revoke(UUID employeeId, String reason) { requireAdmin(); EmployeeQrCode c=codes.findByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID, employeeId, EmployeeQrCode.Status.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("O colaborador não possui QR Code ativo.")); c.revoke(currentUser.requireCurrentUser().userId(), reason, clock.instant()); audit.record(new AuditPort.AuditRecord(currentUser.requireCurrentUser().userId(), "QR_CODE_REVOKED", "EMPLOYEE_QR_CODE", c.getId(), clock.instant(), java.util.Map.of("reason", reason))); return response(c, null); }
	@Transactional(readOnly = true) public byte[] image(UUID employeeId) { QrCodeResponse value=get(employeeId); try { BitMatrix matrix=new MultiFormatWriter().encode(value.verificationUrl().toString(), BarcodeFormat.QR_CODE, 320, 320); ByteArrayOutputStream out=new ByteArrayOutputStream(); MatrixToImageWriter.writeToStream(matrix, "PNG", out); return out.toByteArray(); } catch (Exception e) { throw new IllegalStateException("Não foi possível gerar a imagem do QR Code", e); } }
	@Transactional public QrVerificationResponse verify(String token) { String tokenHash=hash(token); EmployeeQrCode c=codes.findByTokenHash(tokenHash).orElse(null); var user=currentUser.requireCurrentUser(); if (c == null) { log(null,user.userId(),"UNKNOWN",tokenHash); throw new ResourceNotFoundException("O QR Code não existe."); } if (c.getStatus() != EmployeeQrCode.Status.ACTIVE) { log(c,user.userId(),"REVOKED",tokenHash); throw new ResourceConflictException("QR_CODE_REVOKED", "O QR Code foi revogado."); } requireEmployeeAccess(c.getEmployeeId()); log(c,user.userId(),"VALID",tokenHash); var employee=employees.requireEmployee(c.getEmployeeId()); var values=completions.findAllByOrganizationIdAndEmployeeIdOrderByCompletedAtDescIdDesc(DEFAULT_ORGANIZATION_ID, c.getEmployeeId()); var trainingValues=values.stream().map(v -> { var t=trainings.summary(v.getTrainingId()); return new QrVerificationResponse.Training(t.name(), t.code(), t.regulatoryStandard(), v.getCompletedAt(), v.getExpirationDate(), "COMPLETED"); }).toList(); var activityValues=qualifications.list(c.getEmployeeId(), null, null, org.springframework.data.domain.Pageable.unpaged()).getContent().stream().map(q -> new QrVerificationResponse.Activity(q.activity().name(), q.status().name(), q.blockingReasons().stream().map(dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationResponse.BlockingReason::trainingName).toList())).toList(); return new QrVerificationResponse(new QrVerificationResponse.Employee(employee.name(), employee.registration(), employee.jobId().toString()), trainingValues, trainingValues.stream().filter(QrVerificationResponse.Training::isRegulatoryStandard).toList(), activityValues); }
	@Transactional(readOnly = true) public Page<QrAccessLogResponse> logs(UUID codeId, Pageable pageable) { requireAdmin(); return logs.findAllByOrganizationIdAndQrCodeIdOrderByQueriedAtDesc(DEFAULT_ORGANIZATION_ID, codeId, pageable).map(l -> new QrAccessLogResponse(l.getId(), l.getQrCodeId(), l.getQueriedByUserId(), l.getQueriedAt(), l.getResult(), l.getRequestId())); }
	private void log(EmployeeQrCode c, UUID user, String result, String hash) { logs.save(new QrCodeAccessLog(DEFAULT_ORGANIZATION_ID, c == null ? null : c.getId(), user, clock.instant(), result, UUID.randomUUID().toString(), hash)); }
	private void requireAdmin() { if (currentUser.requireCurrentUser().role() != dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN) throw new AccessDeniedException("Acesso restrito ao administrador."); }
	private void requireEmployeeAccess(UUID id) { if (!authorization.canAccessEmployee(id)) throw new AccessDeniedException("Colaborador fora do escopo autorizado."); }
	private QrCodeResponse response(EmployeeQrCode c, String token) { return new QrCodeResponse(c.getId(), c.getEmployeeId(), token, token == null ? null : qrProperties.verificationUrl(token), c.getStatus(), c.getGeneratedAt(), c.getRevokedAt(), c.getRevocationReason()); }
	private String randomToken() { byte[] b=new byte[32]; random.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
	private String hash(String token) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
	private String encrypt(String value) { try { byte[] iv=new byte[12]; random.nextBytes(iv); Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(jwt.signingKey()), "AES"), new GCMParameterSpec(128, iv)); return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
	private String decrypt(String value) { try { String[] p=value.split(":",2); byte[] iv=Base64.getDecoder().decode(p[0]); Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(jwt.signingKey()), "AES"), new GCMParameterSpec(128, iv)); return new String(c.doFinal(Base64.getDecoder().decode(p[1])), StandardCharsets.UTF_8); } catch(Exception e) { throw new IllegalStateException("QR Code indisponível", e); } }
}
