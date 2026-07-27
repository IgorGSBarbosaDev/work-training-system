package dev.igorbarbosa.worktrainingsystem.certificates.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assessments.application.TrainingOutcomeEvent;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateDownloadResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateJobResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateValidationResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.ExternalCertificateRequest;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.Certificate;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateGenerationJob;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateGenerationStatus;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateHistory;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateHistoryType;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateStatus;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateType;
import dev.igorbarbosa.worktrainingsystem.certificates.persistence.CertificateGenerationJobRepository;
import dev.igorbarbosa.worktrainingsystem.certificates.persistence.CertificateHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.certificates.persistence.CertificateRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.files.application.FileService;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuditPort;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CertificateService {
	private final CertificateRepository certificates;
	private final CertificateHistoryRepository history;
	private final CertificateGenerationJobRepository jobs;
	private final TrainingCompletionRepository completions;
	private final TrainingAssignmentRepository assignments;
	private final TrainingCatalog trainings;
	private final EmployeeActivityCatalog employees;
	private final FileService files;
	private final ObjectStorage storage;
	private final AuthorizationService authorization;
	private final CurrentUserProvider currentUser;
	private final AuditPort audit;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public CertificateService(CertificateRepository certificates, CertificateHistoryRepository history,
			CertificateGenerationJobRepository jobs, TrainingCompletionRepository completions,
			TrainingCatalog trainings, TrainingAssignmentRepository assignments,
			EmployeeActivityCatalog employees, FileService files, ObjectStorage storage,
			AuthorizationService authorization, CurrentUserProvider currentUser, AuditPort audit, Clock clock) {
		this.certificates = certificates;
		this.history = history;
		this.jobs = jobs;
		this.completions = completions;
		this.trainings = trainings;
		this.assignments = assignments;
		this.employees = employees;
		this.files = files;
		this.storage = storage;
		this.authorization = authorization;
		this.currentUser = currentUser;
		this.audit = audit;
		this.clock = clock;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onCompletion(TrainingOutcomeEvent event) {
		if (event.completionId() != null) {
			generateInternal(event.completionId(), responsible(event.completionId()), null);
		}
	}

	@Transactional(readOnly = true)
	public Page<CertificateResponse> list(Pageable pageable) {
		return certificates.findAll(visible(), pageable).map(this::response);
	}

	@Transactional(readOnly = true)
	public Page<CertificateResponse> listMine(Pageable pageable) {
		var user = currentUser.requireCurrentUser();
		if (user.employeeId() == null) {
			throw new AccessDeniedException("Usuário não está vinculado a colaborador.");
		}
		return certificates.findAll(visible().and((root, query, builder) -> {
			var subquery = query.subquery(UUID.class);
			var completion = subquery.from(TrainingCompletion.class);
			subquery.select(completion.get("id"))
				.where(builder.equal(completion.get("employeeId"), user.employeeId()));
			return root.get("completionId").in(subquery);
		}), pageable).map(this::response);
	}

	@Transactional(readOnly = true)
	public CertificateResponse get(UUID id) {
		return response(requireVisible(id));
	}

	@Transactional
	public CertificateJobResponse regenerate(UUID id) {
		requireAdmin();
		Certificate old = certificates.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> missing(id));
		var userId = currentUser.requireCurrentUser().userId();
		old.revoke(userId, "regenerated", clock.instant());
		return generateInternal(old.getCompletionId(), userId, old);
	}

	@Transactional
	public CertificateResponse revoke(UUID id, String reason) {
		requireAdmin();
		var userId = currentUser.requireCurrentUser().userId();
		Certificate value = certificates.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> missing(id));
		Instant now = clock.instant();
		value.revoke(userId, reason, now);
		history.save(new CertificateHistory(DEFAULT_ORGANIZATION_ID, id, CertificateHistoryType.REVOKED,
				userId, null, reason, now));
		audit.record(new AuditPort.AuditRecord(userId, "CERTIFICATE_REVOKED", "CERTIFICATE", id, now,
				Map.of("reason", reason)));
		return response(value);
	}

	@Transactional(readOnly = true)
	public CertificateDownloadResponse download(UUID id) {
		Certificate value = requireVisible(id);
		var url = storage.presignDownload(value.getObjectKey());
		return new CertificateDownloadResponse(url.url(), url.expiresAt());
	}

	@Transactional(readOnly = true)
	public CertificateValidationResponse validate(String code) {
		Certificate value = certificates.findByValidationCode(code)
				.orElseThrow(() -> new ResourceNotFoundException("O código de validação não existe."));
		TrainingCompletion completion = completions.findByIdAndOrganizationId(value.getCompletionId(),
				DEFAULT_ORGANIZATION_ID).orElseThrow();
		var training = trainings.summary(completion.getTrainingId());
		var employee = employees.requireEmployee(completion.getEmployeeId());
		return new CertificateValidationResponse(value.getStatus() == CertificateStatus.ACTIVE,
				value.getStatus(), training.name(), masked(employee.name()), employee.registration(),
				completion.getCompletionDate(), completion.getExpirationDate(), value.getIssuedDate());
	}

	@Transactional
	public CertificateResponse external(UUID completionId, ExternalCertificateRequest request) {
		var user = currentUser.requireCurrentUser();
		TrainingCompletion completion = completions.findByIdAndOrganizationId(completionId,
				DEFAULT_ORGANIZATION_ID).orElseThrow(() -> new ResourceNotFoundException("A conclusão não existe."));
		if (user.role() != dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN
				|| !authorization.canAccessEmployee(completion.getEmployeeId())) {
			throw new AccessDeniedException("Acesso restrito ao administrador.");
		}
		var ref = files.requireExternalCertificate(request.fileId(), completion.getEmployeeId());
		Certificate value = new Certificate(DEFAULT_ORGANIZATION_ID, completionId, CertificateType.EXTERNAL,
				token(), ref.objectKey(), clock.instant(), user.userId(), null,
				certificates.countByOrganizationIdAndCompletionIdAndType(DEFAULT_ORGANIZATION_ID, completionId,
						CertificateType.EXTERNAL) + 1);
		certificates.save(value);
		history.save(new CertificateHistory(DEFAULT_ORGANIZATION_ID, value.getId(), CertificateHistoryType.ISSUED,
				user.userId(), null, "external", value.getIssuedAt()));
		return response(value);
	}

	private CertificateJobResponse generateInternal(UUID completionId, UUID actor, Certificate replaces) {
		TrainingCompletion completion = completions.findByIdAndOrganizationId(completionId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow();
		var active = certificates.findByOrganizationIdAndCompletionIdAndTypeAndStatus(DEFAULT_ORGANIZATION_ID,
				completionId, CertificateType.INTERNAL, CertificateStatus.ACTIVE);
		if (replaces == null && active.isPresent()) {
			return new CertificateJobResponse(UUID.randomUUID(), completionId, CertificateGenerationStatus.COMPLETED,
					0, null, active.get().getId(), active.get().getIssuedAt());
		}
		Instant now = clock.instant();
		CertificateGenerationJob job = jobs.save(new CertificateGenerationJob(DEFAULT_ORGANIZATION_ID, completionId,
				CertificateType.INTERNAL, actor, replaces == null ? null : replaces.getId(), now));
		job.processing(now);
		var training = trainings.summary(completion.getTrainingId());
		var employee = employees.requireEmployee(completion.getEmployeeId());
		String key = "organizations/%s/certificates/%s.pdf".formatted(DEFAULT_ORGANIZATION_ID, UUID.randomUUID());
		byte[] pdf = pdf(completion, training.name(), employee.name(), employee.registration());
		storage.upload(key, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");
		int generation = certificates.countByOrganizationIdAndCompletionIdAndType(DEFAULT_ORGANIZATION_ID,
				completionId, CertificateType.INTERNAL) + 1;
		Certificate value = new Certificate(DEFAULT_ORGANIZATION_ID, completionId, CertificateType.INTERNAL, token(), key,
				now, actor, replaces == null ? null : replaces.getId(), generation);
		certificates.save(value);
		history.save(new CertificateHistory(DEFAULT_ORGANIZATION_ID, value.getId(),
				replaces == null ? CertificateHistoryType.ISSUED : CertificateHistoryType.REGENERATED, actor,
				replaces == null ? null : replaces.getId(), null, now));
		job.completed(value.getId(), now);
		audit.record(new AuditPort.AuditRecord(actor, "CERTIFICATE_ISSUED", "CERTIFICATE", value.getId(), now,
				Map.of("type", "INTERNAL")));
		return new CertificateJobResponse(job.getId(), completionId, job.getStatus(), job.getAttemptCount(),
				job.getLastError(), value.getId(), now);
	}

	private byte[] pdf(TrainingCompletion completion, String trainingName, String employeeName, String registration) {
		try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (var content = new PDPageContentStream(document, page)) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
				content.newLineAtOffset(72, 700);
				content.showText("Work Training System");
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(0, -42);
				content.showText("Certificate of completion");
				content.newLineAtOffset(0, -32);
				content.showText("Employee: " + safePdfText(employeeName));
				content.newLineAtOffset(0, -22);
				content.showText("Registration: " + safePdfText(registration));
				content.newLineAtOffset(0, -22);
				content.showText("Training: " + safePdfText(trainingName));
				content.newLineAtOffset(0, -22);
				content.showText("Completed: " + completion.getCompletionDate());
				if (completion.getExpirationDate() != null) {
					content.newLineAtOffset(0, -22);
					content.showText("Expires: " + completion.getExpirationDate());
				}
				content.endText();
			}
			document.save(output);
			return output.toByteArray();
		} catch (Exception exception) {
			throw new IllegalStateException("Could not generate certificate PDF", exception);
		}
	}

	private UUID responsible(UUID completionId) {
		TrainingCompletion completion = completions.findByIdAndOrganizationId(completionId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow();
		if (completion.getResponsibleUserId() != null) return completion.getResponsibleUserId();
		if (completion.getSourceAssignmentId() != null) {
			return assignments.findByIdAndOrganizationId(completion.getSourceAssignmentId(), DEFAULT_ORGANIZATION_ID)
					.orElseThrow().getResponsibleUserId();
		}
		throw new IllegalStateException("Completion has no certificate responsible user");
	}

	private Certificate requireVisible(UUID id) {
		Certificate value = certificates.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> missing(id));
		TrainingCompletion completion = completions.findByIdAndOrganizationId(value.getCompletionId(),
				DEFAULT_ORGANIZATION_ID).orElseThrow();
		if (!authorization.canAccessEmployee(completion.getEmployeeId())
				&& currentUser.requireCurrentUser().role() != dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN) {
			throw new AccessDeniedException("Certificado fora do escopo autorizado.");
		}
		return value;
	}

	private Specification<Certificate> visible() {
		var scope = authorization.currentScope();
		Specification<Certificate> spec = (root, query, builder) -> builder.equal(root.get("organizationId"),
				DEFAULT_ORGANIZATION_ID);
		if (scope.admin()) return spec;
		if (scope.employee()) {
			return spec.and((root, query, builder) -> {
				var subquery = query.subquery(UUID.class);
				var completion = subquery.from(TrainingCompletion.class);
				subquery.select(completion.get("id"))
						.where(builder.equal(completion.get("employeeId"), scope.ownEmployeeId()));
				return root.get("completionId").in(subquery);
			});
		}
		Set<UUID> ids = authorization.scopeReferences(scope).employeeIds();
		return spec.and((root, query, builder) -> {
			var subquery = query.subquery(UUID.class);
			var completion = subquery.from(TrainingCompletion.class);
			subquery.select(completion.get("id")).where(completion.get("employeeId").in(ids));
			return root.get("completionId").in(subquery);
		});
	}

	private void requireAdmin() {
		if (currentUser.requireCurrentUser().role() != dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN) {
			throw new AccessDeniedException("Acesso restrito ao administrador.");
		}
	}

	private CertificateResponse response(Certificate value) {
		return new CertificateResponse(value.getId(), value.getCompletionId(), value.getType(), value.getValidationCode(),
				value.getIssuedDate(), value.getIssuedAt(), value.getStatus(), value.getResponsibleUserId(),
				value.getRevokedAt(), value.getRevokedByUserId(), value.getRevocationReason(),
				value.getPreviousCertificateId(), value.getGenerationNumber());
	}

	private String token() {
		byte[] bytes = new byte[24];
		random.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	private String safePdfText(String value) {
		return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
	}

	private String masked(String value) {
		if (value == null || value.length() < 3) return "***";
		return value.charAt(0) + "***" + value.charAt(value.length() - 1);
	}

	private ResourceNotFoundException missing(UUID id) {
		return new ResourceNotFoundException("O certificado informado não existe.");
	}
}
