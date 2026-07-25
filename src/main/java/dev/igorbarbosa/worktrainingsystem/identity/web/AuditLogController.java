package dev.igorbarbosa.worktrainingsystem.identity.web;

import dev.igorbarbosa.worktrainingsystem.identity.api.AuditLogResponse;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuditQueryService;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/audit-logs") @PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
	private static final Set<String> SORT=Set.of("occurredAt","action","entityType"); private final AuditQueryService service; private final PaginationFactory pagination;
	public AuditLogController(AuditQueryService service,PaginationFactory pagination){this.service=service;this.pagination=pagination;}
	@GetMapping public PageResponse<AuditLogResponse> list(@RequestParam(required=false)UUID userId,@RequestParam(required=false)String action,@RequestParam(required=false)String entityType,@RequestParam(required=false)UUID entityId,@RequestParam(required=false)Instant occurredFrom,@RequestParam(required=false)Instant occurredTo,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="occurredAt,desc")String sort){return PageResponse.from(service.list(userId,action,entityType,entityId,occurredFrom,occurredTo,pagination.create(page,size,sort,SORT)));}
	@GetMapping("/{id}") public AuditLogResponse get(@PathVariable UUID id){return service.get(id);}
}
