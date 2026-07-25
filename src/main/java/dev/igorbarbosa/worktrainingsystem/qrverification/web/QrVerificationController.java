package dev.igorbarbosa.worktrainingsystem.qrverification.web;

import dev.igorbarbosa.worktrainingsystem.qrverification.api.QrAccessLogResponse;
import dev.igorbarbosa.worktrainingsystem.qrverification.api.QrCodeResponse;
import dev.igorbarbosa.worktrainingsystem.qrverification.api.QrVerificationResponse;
import dev.igorbarbosa.worktrainingsystem.qrverification.application.QrVerificationService;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
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
public class QrVerificationController {
	private static final Set<String> SORT=Set.of("queriedAt", "result"); private final QrVerificationService service; private final PaginationFactory pagination;
	public QrVerificationController(QrVerificationService service, PaginationFactory pagination){this.service=service;this.pagination=pagination;}
	@GetMapping("/employees/{employeeId}/qr-code") public QrCodeResponse get(@PathVariable UUID employeeId){return service.get(employeeId);}
	@GetMapping("/me/qr-code") @PreAuthorize("hasRole('EMPLOYEE')") public QrCodeResponse mine(){return service.getMine();}
	@PostMapping("/employees/{employeeId}/qr-code") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<QrCodeResponse> generate(@PathVariable UUID employeeId){var value=service.generate(employeeId);return ResponseEntity.status(201).body(value);}
	@PostMapping("/employees/{employeeId}/qr-code/revoke") @PreAuthorize("hasRole('ADMIN')") public QrCodeResponse revoke(@PathVariable UUID employeeId,@RequestBody RevocationRequest request){return service.revoke(employeeId,request.reason());}
	@GetMapping(value="/employees/{employeeId}/qr-code/image", produces=MediaType.IMAGE_PNG_VALUE) public byte[] image(@PathVariable UUID employeeId){return service.image(employeeId);}
	@GetMapping("/qr-verifications/{token}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')") public QrVerificationResponse verify(@PathVariable @NotBlank String token){return service.verify(token);}
	@GetMapping("/qr-verifications/{codeId}/access-log") @PreAuthorize("hasRole('ADMIN')") public PageResponse<QrAccessLogResponse> logs(@PathVariable UUID codeId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="queriedAt,desc") String sort){return PageResponse.from(service.logs(codeId,pagination.create(page,size,sort,SORT)));}
	public record RevocationRequest(@NotBlank String reason){}
}
