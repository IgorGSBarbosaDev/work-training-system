package dev.igorbarbosa.worktrainingsystem.files.web;

import dev.igorbarbosa.worktrainingsystem.files.api.FileUrlResponse;
import dev.igorbarbosa.worktrainingsystem.files.api.UploadRequest;
import dev.igorbarbosa.worktrainingsystem.files.api.UploadResponse;
import dev.igorbarbosa.worktrainingsystem.files.application.FileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FileController {
	private final FileService service;
	public FileController(FileService service) { this.service = service; }
	@PostMapping("/uploads") @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
	public ResponseEntity<UploadResponse> request(@Valid @RequestBody UploadRequest request) {
		UploadResponse response = service.request(request);
		return ResponseEntity.created(URI.create("/api/v1/uploads/" + response.uploadId())).body(response);
	}
	@PostMapping("/uploads/{uploadId}/complete") @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
	public UploadResponse complete(@PathVariable UUID uploadId) { return service.complete(uploadId); }
	@DeleteMapping("/uploads/{uploadId}") @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
	public ResponseEntity<Void> cancel(@PathVariable UUID uploadId) { service.cancel(uploadId); return ResponseEntity.noContent().build(); }
	@GetMapping("/files/{fileId}/download-url") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public FileUrlResponse download(@PathVariable UUID fileId) { return service.downloadUrl(fileId); }
}
