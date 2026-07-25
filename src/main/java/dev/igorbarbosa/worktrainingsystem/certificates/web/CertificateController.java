package dev.igorbarbosa.worktrainingsystem.certificates.web;

import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateDownloadResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateJobResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateRevocationRequest;
import dev.igorbarbosa.worktrainingsystem.certificates.api.CertificateValidationResponse;
import dev.igorbarbosa.worktrainingsystem.certificates.api.ExternalCertificateRequest;
import dev.igorbarbosa.worktrainingsystem.certificates.application.CertificateService;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1")
public class CertificateController {
	private static final Set<String> SORT = Set.of("issuedAt", "issuedDate", "generationNumber", "status");
	private final CertificateService service; private final PaginationFactory pagination;
	public CertificateController(CertificateService service, PaginationFactory pagination) { this.service = service; this.pagination = pagination; }
	@GetMapping("/certificates") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<CertificateResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "issuedAt,desc") String sort) { return PageResponse.from(service.list(pagination.create(page, size, sort, SORT))); }
	@GetMapping("/me/certificates") @PreAuthorize("hasRole('EMPLOYEE')")
	public PageResponse<CertificateResponse> mine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "issuedAt,desc") String sort) { return PageResponse.from(service.listMine(pagination.create(page, size, sort, SORT))); }
	@GetMapping("/certificates/{id}") public CertificateResponse get(@PathVariable UUID id) { return service.get(id); }
	@GetMapping("/certificates/{id}/download") public CertificateDownloadResponse download(@PathVariable UUID id) { return service.download(id); }
	@PostMapping("/certificates/{id}/revoke") @PreAuthorize("hasRole('ADMIN')") public CertificateResponse revoke(@PathVariable UUID id, @Valid @RequestBody CertificateRevocationRequest request) { return service.revoke(id, request.reason()); }
	@PostMapping("/certificates/{id}/regenerate") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<CertificateJobResponse> regenerate(@PathVariable UUID id) { return ResponseEntity.accepted().body(service.regenerate(id)); }
	@PostMapping("/training-completions/{completionId}/certificate") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<CertificateResponse> external(@PathVariable UUID completionId, @Valid @RequestBody ExternalCertificateRequest request) { CertificateResponse response = service.external(completionId, request); return ResponseEntity.created(URI.create("/api/v1/certificates/" + response.id())).body(response); }
	@GetMapping("/certificate-validations/{validationCode}") public CertificateValidationResponse validate(@PathVariable String validationCode) { return service.validate(validationCode); }
}
