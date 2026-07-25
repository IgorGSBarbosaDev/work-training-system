package dev.igorbarbosa.worktrainingsystem.notifications.web;

import dev.igorbarbosa.worktrainingsystem.notifications.api.EmailDeliveryResponse;
import dev.igorbarbosa.worktrainingsystem.notifications.api.NotificationResponse;
import dev.igorbarbosa.worktrainingsystem.notifications.application.NotificationService;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1")
public class NotificationController {
	private static final Set<String> SORT = Set.of("createdAt", "updatedAt", "status");
	private final NotificationService service; private final PaginationFactory pagination;
	public NotificationController(NotificationService service, PaginationFactory pagination) { this.service = service; this.pagination = pagination; }
	@GetMapping("/me/notifications") public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "createdAt,desc") String sort) {
		return PageResponse.from(service.list(pagination.create(page, size, sort, SORT)).map(this::notification));
	}
	@GetMapping("/me/notifications/unread-count") public java.util.Map<String, Long> unreadCount() { return java.util.Map.of("count", service.unreadCount()); }
	@PatchMapping("/me/notifications/{id}/read") public NotificationResponse read(@PathVariable UUID id) { return notification(service.read(id)); }
	@PatchMapping("/me/notifications/read-all") public ResponseEntity<Void> readAll() { service.readAll(); return ResponseEntity.noContent().build(); }
	@PatchMapping("/me/notifications/{id}/archive") public NotificationResponse archive(@PathVariable UUID id) { return notification(service.archive(id)); }
	@GetMapping("/admin/email-deliveries") @PreAuthorize("hasRole('ADMIN')") public PageResponse<EmailDeliveryResponse> deliveries(@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "updatedAt,desc") String sort) {
		return PageResponse.from(service.deliveries(pagination.create(page, size, sort, SORT)).map(this::delivery));
	}
	@PostMapping("/admin/email-deliveries/{id}/retry") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<EmailDeliveryResponse> retry(@PathVariable UUID id) { return ResponseEntity.accepted().body(delivery(service.retryDelivery(id))); }
	private NotificationResponse notification(dev.igorbarbosa.worktrainingsystem.notifications.domain.Notification n) { return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getRelatedEntityType(), n.getRelatedEntityId(), n.getCreatedAt(), n.getReadAt(), n.getArchivedAt()); }
	private EmailDeliveryResponse delivery(dev.igorbarbosa.worktrainingsystem.notifications.domain.EmailDelivery d) { return new EmailDeliveryResponse(d.getId(), d.getNotificationId(), d.getRecipient(), d.getSubject(), d.getStatus(), d.getAttemptCount(), d.getLastError(), d.getCreatedAt(), d.getUpdatedAt()); }
}
