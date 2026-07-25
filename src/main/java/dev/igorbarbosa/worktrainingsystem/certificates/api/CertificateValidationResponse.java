package dev.igorbarbosa.worktrainingsystem.certificates.api;

import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateStatus;
import java.time.LocalDate;

public record CertificateValidationResponse(boolean valid, CertificateStatus status, String trainingName,
		String employeeName, String employeeRegistration, LocalDate completedAt, LocalDate expiresAt,
		LocalDate issuedAt) {}
