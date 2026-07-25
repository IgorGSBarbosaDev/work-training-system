package dev.igorbarbosa.worktrainingsystem.notifications.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import dev.igorbarbosa.worktrainingsystem.notifications.domain.EmailDelivery;
import dev.igorbarbosa.worktrainingsystem.notifications.domain.Notification;
import dev.igorbarbosa.worktrainingsystem.notifications.persistence.EmailDeliveryRepository;
import dev.igorbarbosa.worktrainingsystem.notifications.persistence.NotificationRepository;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService implements SliceBNotificationPort {
	private final NotificationRepository notifications; private final EmailDeliveryRepository deliveries;
	private final UserRepository users; private final CurrentUserProvider currentUser; private final Clock clock;
	public NotificationService(NotificationRepository notifications, EmailDeliveryRepository deliveries, UserRepository users,
			CurrentUserProvider currentUser, Clock clock) {
		this.notifications = notifications; this.deliveries = deliveries; this.users = users; this.currentUser = currentUser; this.clock = clock;
	}
	@Transactional(readOnly = true)
	public Page<Notification> list(Pageable pageable) { var user = currentUser.requireCurrentUser(); return notifications.findAllByOrganizationIdAndUserIdAndArchivedAtIsNull(user.organizationId(), user.userId(), pageable); }
	@Transactional(readOnly = true)
	public long unreadCount() { var user = currentUser.requireCurrentUser(); return notifications.countByOrganizationIdAndUserIdAndReadAtIsNullAndArchivedAtIsNull(user.organizationId(), user.userId()); }
	@Transactional public Notification read(UUID id) { return change(id, true, false); }
	@Transactional public Notification archive(UUID id) { return change(id, false, true); }
	@Transactional public void readAll() { list(org.springframework.data.domain.Pageable.unpaged()).forEach(n -> n.read(clock.instant())); }
	@Transactional(readOnly = true) public Page<EmailDelivery> deliveries(Pageable pageable) { requireAdmin(); return deliveries.findAllByOrganizationId(DEFAULT_ORGANIZATION_ID, pageable); }
	@Transactional public EmailDelivery retryDelivery(UUID id) { requireAdmin(); var d = deliveries.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).orElseThrow(() -> new ResourceNotFoundException("A entrega não existe.")); d.retry(clock.instant()); return d; }
	@Override @Transactional public void expirationChanged(ExpirationNotification event) { create(event.organizationId(), event.employeeId(), "EXPIRATION_" + event.status(), "Treinamento próximo do vencimento", "O treinamento vence em " + event.expirationDate(), "COMPLETION", event.completionId()); }
	@Override @Transactional public void qualificationBlocked(QualificationBlockedNotification event) { create(event.organizationId(), event.employeeId(), "QUALIFICATION_BLOCKED", "Atividade bloqueada", "Existe uma pendência de treinamento para uma atividade.", "ACTIVITY", event.activityId()); }
	@Override @Transactional public void assignmentCreated(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "ASSIGNMENT_CREATED", "Novo treinamento atribuído", "Você recebeu um novo treinamento.", "ASSIGNMENT", event.assignmentId()); }
	@Override @Transactional public void assignmentDue(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "ASSIGNMENT_DUE", "Treinamento pendente", "Existe um treinamento com prazo próximo.", "ASSIGNMENT", event.assignmentId()); }
	@Override @Transactional public void assessmentFailed(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "ASSESSMENT_FAILED", "Avaliação não aprovada", "A avaliação do treinamento precisa ser refeita.", "ASSIGNMENT", event.assignmentId()); }
	@Override @Transactional public void trainingCompleted(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "TRAINING_COMPLETED", "Treinamento concluído", "Seu treinamento foi concluído.", "ASSIGNMENT", event.assignmentId()); }
	private void create(UUID organizationId, UUID employeeId, String type, String title, String message, String entityType, UUID entityId) {
		var user = users.findAllByOrganizationId(organizationId, Pageable.unpaged()).stream().filter(u -> employeeId.equals(u.getEmployeeId())).findFirst().orElse(null);
		if (user == null) return; Instant now = clock.instant(); var n = notifications.save(new Notification(organizationId, user.getId(), type, title, message, entityType, entityId, now));
		deliveries.save(new EmailDelivery(organizationId, user.getId(), n.getId(), user.getEmail(), title, now));
	}
	private Notification change(UUID id, boolean read, boolean archive) { var user = currentUser.requireCurrentUser(); var n = notifications.findById(id).filter(v -> v.getOrganizationId().equals(user.organizationId()) && v.getUserId().equals(user.userId())).orElseThrow(() -> new ResourceNotFoundException("A notificação não existe.")); if (read) n.read(clock.instant()); if (archive) n.archive(clock.instant()); return n; }
	private void requireAdmin() { if (currentUser.requireCurrentUser().role() != dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN) throw new org.springframework.security.access.AccessDeniedException("Acesso restrito ao administrador."); }
}
