package dev.igorbarbosa.worktrainingsystem.identity.web;

import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ChangeUserStatusRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.CreateUserRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.PermissionsResponse;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UpdatePermissionsRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UpdateUserRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UserResponse;
import dev.igorbarbosa.worktrainingsystem.identity.application.UserAdministrationService;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
	private static final Set<String> SORTABLE = Set.of("email", "role", "status", "createdAt", "updatedAt");
	private final UserAdministrationService users;
	private final PaginationFactory pagination;
	public UserController(UserAdministrationService users, PaginationFactory pagination) { this.users = users; this.pagination = pagination; }

	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		UserResponse response = users.create(request);
		return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
	}
	@GetMapping
	public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole role,
			@RequestParam(required = false) dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus status) {
		return PageResponse.from(users.list(search, role, status, pagination.create(page, size, sort, SORTABLE)));
	}
	@GetMapping("/{userId}") public UserResponse get(@PathVariable UUID userId) { return users.get(userId); }
	@PatchMapping("/{userId}") public UserResponse update(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) { return users.update(userId, request); }
	@PatchMapping("/{userId}/status") public UserResponse status(@PathVariable UUID userId, @Valid @RequestBody ChangeUserStatusRequest request) { return users.changeStatus(userId, request.status()); }
	@PostMapping("/{userId}/password-reset") public ResponseEntity<Void> passwordReset(@PathVariable UUID userId) { users.requestPasswordReset(userId); return ResponseEntity.accepted().build(); }
	@GetMapping("/{userId}/permissions") public PermissionsResponse permissions(@PathVariable UUID userId) { return users.getPermissions(userId); }
	@PatchMapping("/{userId}/permissions") public PermissionsResponse permissions(@PathVariable UUID userId, @Valid @RequestBody UpdatePermissionsRequest request) { return users.updatePermissions(userId, request); }
}
