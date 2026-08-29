package dev.igorbarbosa.worktrainingsystem.notifications.application;

import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import dev.igorbarbosa.worktrainingsystem.notifications.domain.EmailDelivery;
import dev.igorbarbosa.worktrainingsystem.notifications.domain.Notification;
import dev.igorbarbosa.worktrainingsystem.notifications.persistence.EmailDeliveryRepository;
import dev.igorbarbosa.worktrainingsystem.notifications.persistence.NotificationRepository;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService implements SliceBNotificationPort {
	private final NotificationRepository notifications; private final EmailDeliveryRepository deliveries;
	private final UserRepository users; private final CurrentUserProvider currentUser; private final Clock clock;
	private final ApplicationEventPublisher events;
	public NotificationService(NotificationRepository notifications, EmailDeliveryRepository deliveries, UserRepository users,
			CurrentUserProvider currentUser, Clock clock, ApplicationEventPublisher events) {
		this.notifications = notifications; this.deliveries = deliveries; this.users = users; this.currentUser = currentUser; this.clock = clock; this.events = events;
	}
	@Transactional(readOnly = true)
	public Page<Notification> list(Pageable pageable) { var user = currentUser.requireCurrentUser(); return notifications.findAllByOrganizationIdAndUserIdAndArchivedAtIsNull(user.organizationId(), user.userId(), pageable); }
	@Transactional(readOnly = true)
	public long unreadCount() { var user = currentUser.requireCurrentUser(); return notifications.countByOrganizationIdAndUserIdAndReadAtIsNullAndArchivedAtIsNull(user.organizationId(), user.userId()); }
	@Transactional public Notification read(UUID id) { return change(id, true, false); }
	@Transactional public Notification archive(UUID id) { return change(id, false, true); }
	@Transactional public void readAll() { list(org.springframework.data.domain.Pageable.unpaged()).forEach(n -> n.read(clock.instant())); }
	@Transactional(readOnly = true) public Page<EmailDelivery> deliveries(EmailDelivery.Status status, String recipient,
			Instant createdFrom, Instant createdTo, Pageable pageable) { var actor=requireAdmin(); Specification<EmailDelivery> specification=(root,query,builder)->builder.equal(root.get("organizationId"),actor.organizationId()); if(status!=null)specification=specification.and((root,query,builder)->builder.equal(root.get("status"),status)); if(recipient!=null&&!recipient.isBlank()){String pattern="%"+recipient.trim().toLowerCase()+"%";specification=specification.and((root,query,builder)->builder.like(builder.lower(root.get("recipient")),pattern));} if(createdFrom!=null)specification=specification.and((root,query,builder)->builder.greaterThanOrEqualTo(root.get("createdAt"),createdFrom)); if(createdTo!=null)specification=specification.and((root,query,builder)->builder.lessThanOrEqualTo(root.get("createdAt"),createdTo)); return deliveries.findAll(specification,pageable); }
	@Transactional public EmailDelivery retryDelivery(UUID id) { var actor=requireAdmin(); var d = deliveries.findByIdAndOrganizationId(id, actor.organizationId()).orElseThrow(() -> new ResourceNotFoundException("A entrega não existe.")); d.retry(clock.instant()); events.publishEvent(new EmailDeliveryDispatcher.EmailDeliveryQueued(actor.organizationId(), d.getId())); return d; }
	@Override @Transactional public void expirationChanged(ExpirationNotification event) { create(event.organizationId(), event.employeeId(), "EXPIRATION_" + event.status(), "Treinamento próximo do vencimento", "O treinamento vence em " + event.expirationDate(), "COMPLETION", event.completionId(), event.expirationDate()); }
	@Override @Transactional public void qualificationBlocked(QualificationBlockedNotification event) { create(event.organizationId(), event.employeeId(), "QUALIFICATION_BLOCKED", "Atividade bloqueada", "Existe uma pendência de treinamento para uma atividade.", "ACTIVITY", event.activityId(), LocalDate.now(clock)); }
	@Override @Transactional public void assignmentCreated(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "ASSIGNMENT_CREATED", "Novo treinamento atribuído", "Você recebeu um novo treinamento.", "ASSIGNMENT", event.assignmentId(), effective(event)); }
	@Override @Transactional public void assignmentDue(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "ASSIGNMENT_DUE", "Prazo de treinamento próximo", "Existe um treinamento com prazo de conclusão próximo.", "ASSIGNMENT", event.assignmentId(), effective(event)); }
	@Override @Transactional public void assessmentFailed(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "ASSESSMENT_FAILED", "Avaliação não aprovada", "A avaliação do treinamento precisa ser refeita.", "ASSIGNMENT", event.assignmentId(), effective(event)); }
	@Override @Transactional public void trainingCompleted(AssignmentNotification event) { create(event.organizationId(), event.employeeId(), "TRAINING_COMPLETED", "Treinamento concluído", "Seu treinamento foi concluído.", "ASSIGNMENT", event.assignmentId(), effective(event)); }
	private void create(UUID organizationId, UUID employeeId, String type, String title, String message, String entityType, UUID entityId, LocalDate effectiveDate) {
		var user = users.findAllByOrganizationId(organizationId, Pageable.unpaged()).stream().filter(u -> employeeId.equals(u.getEmployeeId())).findFirst().orElse(null);
		if (user == null) return;
		String deduplicationKey = entityType + ":" + entityId + ":" + effectiveDate;
		if (notifications.existsByOrganizationIdAndUserIdAndTypeAndDeduplicationKey(organizationId, user.getId(), type, deduplicationKey)) return;
		Instant now = clock.instant(); var n = notifications.save(new Notification(organizationId, user.getId(), type, title, message, entityType, entityId, deduplicationKey, now));
		var delivery = deliveries.save(new EmailDelivery(organizationId, user.getId(), n.getId(), user.getEmail(), title, message, now));
		events.publishEvent(new EmailDeliveryDispatcher.EmailDeliveryQueued(organizationId, delivery.getId()));
	}
	private LocalDate effective(AssignmentNotification event) { return event.effectiveDate() == null ? LocalDate.now(clock) : event.effectiveDate(); }
	private Notification change(UUID id, boolean read, boolean archive) { var user = currentUser.requireCurrentUser(); var n = notifications.findById(id).filter(v -> v.getOrganizationId().equals(user.organizationId()) && v.getUserId().equals(user.userId())).orElseThrow(() -> new ResourceNotFoundException("A notificação não existe.")); if (read) n.read(clock.instant()); if (archive) n.archive(clock.instant()); return n; }
	private dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser requireAdmin() { var actor=currentUser.requireCurrentUser(); if (actor.role() != dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN) throw new org.springframework.security.access.AccessDeniedException("Acesso restrito ao administrador."); return actor; }
}
