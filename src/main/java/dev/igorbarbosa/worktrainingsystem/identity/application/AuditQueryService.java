package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.api.AuditLogResponse;
import dev.igorbarbosa.worktrainingsystem.identity.domain.AuditLog;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class AuditQueryService {
	private final AuditLogRepository logs; private final CurrentUserProvider currentUser;
	public AuditQueryService(AuditLogRepository logs, CurrentUserProvider currentUser){this.logs=logs;this.currentUser=currentUser;}
	@Transactional(readOnly=true) public Page<AuditLogResponse> list(UUID userId,String action,String entityType,UUID entityId,Instant from,Instant to,Pageable pageable){var actor=requireAdmin(); Specification<AuditLog> s=organization(actor.organizationId()); if(userId!=null)s=s.and(eq("userId",userId)); if(action!=null)s=s.and(eq("action",action)); if(entityType!=null)s=s.and(eq("entityType",entityType)); if(entityId!=null)s=s.and(eq("entityId",entityId)); if(from!=null)s=s.and((r,q,c)->c.greaterThanOrEqualTo(r.get("occurredAt"),from)); if(to!=null)s=s.and((r,q,c)->c.lessThanOrEqualTo(r.get("occurredAt"),to)); return logs.findAll(s,pageable).map(v->new AuditLogResponse(v.getId(),v.getUserId(),v.getAction(),v.getEntityType(),v.getEntityId(),v.getOccurredAt(),v.getRequestId(),v.getDetails()));}
	@Transactional(readOnly=true) public AuditLogResponse get(UUID id){var actor=requireAdmin(); AuditLog v=logs.findById(id).filter(x->actor.organizationId().equals(x.getOrganizationId())).orElseThrow(()->new dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException("O log não existe.")); return new AuditLogResponse(v.getId(),v.getUserId(),v.getAction(),v.getEntityType(),v.getEntityId(),v.getOccurredAt(),v.getRequestId(),v.getDetails());}
	private Specification<AuditLog> organization(UUID organizationId){return (r,q,c)->c.equal(r.get("organizationId"),organizationId);} private Specification<AuditLog> eq(String p,Object v){return (r,q,c)->c.equal(r.get(p),v);} private CurrentUser requireAdmin(){var actor=currentUser.requireCurrentUser();if(actor.role()!=dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN)throw new AccessDeniedException("Acesso restrito ao administrador.");return actor;}
}
