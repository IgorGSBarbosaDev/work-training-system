package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.CreateUserRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.PermissionsResponse;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ScopeGrantRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UpdatePermissionsRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UpdateUserRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UserResponse;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuditPort.AuditRecord;
import dev.igorbarbosa.worktrainingsystem.identity.domain.AccessScopeGrant;
import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserPermission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.AccessScopeGrantRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserPermissionRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdministrationService {
	private final UserRepository users;
	private final UserPermissionRepository permissions;
	private final AccessScopeGrantRepository scopes;
	private final EmployeeIdentityPort employees;
	private final OrganizationIdentityPort organization;
	private final AuthenticationService authenticationService;
	private final PasswordEncoder passwordEncoder;
	private final OpaqueTokenService opaqueTokens;
	private final CurrentUserProvider currentUserProvider;
	private final AuditPort audit;
	private final Clock clock;

	public UserAdministrationService(UserRepository users, UserPermissionRepository permissions,
			AccessScopeGrantRepository scopes, EmployeeIdentityPort employees,
			OrganizationIdentityPort organization,
			AuthenticationService authenticationService, PasswordEncoder passwordEncoder,
			OpaqueTokenService opaqueTokens, CurrentUserProvider currentUserProvider, AuditPort audit, Clock clock) {
		this.users = users; this.permissions = permissions; this.scopes = scopes; this.employees = employees;
		this.organization = organization;
		this.authenticationService = authenticationService; this.passwordEncoder = passwordEncoder;
		this.opaqueTokens = opaqueTokens; this.currentUserProvider = currentUserProvider; this.audit = audit; this.clock = clock;
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		ensureEmailAvailable(request.email(), null);
		validateEmployeeLink(request.role(), request.employeeId());
		User user = users.save(new User(organizationId(), request.email(),
				passwordEncoder.encode(opaqueTokens.generate()), request.role(), UserStatus.ACTIVE,
				request.employeeId(), clock.instant()));
		if (request.sendActivationEmail()) authenticationService.requestResetFor(user);
		audit("USER_CREATED", user, Map.of("role", user.getRole().name()));
		return response(user);
	}

	@Transactional(readOnly = true)
	public UserResponse get(UUID id) { return response(find(id)); }

	@Transactional(readOnly = true)
	public Page<UserResponse> list(String search, UserRole role, UserStatus status, Pageable pageable) {
		Specification<User> specification = (root, query, builder) -> builder.equal(root.get("organizationId"), organizationId());
		if (search != null && !search.isBlank()) {
			String pattern = "%" + search.trim().toLowerCase() + "%";
			specification = specification.and((root, query, builder) -> builder.like(builder.lower(root.get("email")), pattern));
		}
		if (role != null) specification = specification.and((root, query, builder) -> builder.equal(root.get("role"), role));
		if (status != null) specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
		return users.findAll(specification, pageable).map(this::response);
	}

	@Transactional
	public UserResponse update(UUID id, UpdateUserRequest request) {
		User user = find(id);
		ensureEmailAvailable(request.email(), id);
		validateEmployeeLink(request.role(), request.employeeId());
		user.update(request.email(), request.role(), request.employeeId());
		authenticationService.revokeAllSessions(user.getId(), "IDENTITY_CHANGED");
		audit("USER_UPDATED", user, Map.of("role", user.getRole().name()));
		return response(user);
	}

	@Transactional
	public UserResponse changeStatus(UUID id, UserStatus status) {
		User user = find(id);
		user.changeStatus(status);
		if (status != UserStatus.ACTIVE) authenticationService.revokeAllSessions(user.getId(), "USER_STATUS_CHANGED");
		audit("USER_STATUS_CHANGED", user, Map.of("status", status.name()));
		return response(user);
	}

	@Transactional
	public void requestPasswordReset(UUID id) {
		User user = find(id);
		authenticationService.requestResetFor(user);
		audit("USER_PASSWORD_RESET_REQUESTED", user, Map.of());
	}

	@Transactional(readOnly = true)
	public PermissionsResponse getPermissions(UUID id) {
		User user = find(id);
		Set<Permission> effective = user.getRole() == UserRole.ADMIN ? EnumSet.allOf(Permission.class)
				: permissions.findAllByUserId(id).stream().map(UserPermission::getPermission).collect(Collectors.toUnmodifiableSet());
		List<ScopeGrantRequest> grants = scopes.findAllByUserIdAndActiveTrue(id).stream()
				.map(grant -> new ScopeGrantRequest(grant.getScopeType(), grant.targetId())).toList();
		return new PermissionsResponse(effective, grants);
	}

	@Transactional
	public PermissionsResponse updatePermissions(UUID id, UpdatePermissionsRequest request) {
		User user = find(id);
		permissions.deleteByUserId(id);
		scopes.deleteByUserId(id);
		if (user.getRole() != UserRole.ADMIN) {
			permissions.saveAll(request.permissions().stream().map(value -> new UserPermission(id, value)).toList());
		}
		validateUniqueScopes(request.scopes());
		scopes.saveAll(request.scopes().stream()
				.map(grant -> new AccessScopeGrant(id, user.getOrganizationId(), grant.type(), grant.targetId())).toList());
		audit("USER_PERMISSIONS_CHANGED", user, Map.of("permissionCount", String.valueOf(request.permissions().size()),
				"scopeCount", String.valueOf(request.scopes().size())));
		return getPermissions(id);
	}

	private void validateUniqueScopes(List<ScopeGrantRequest> values) {
		long distinct = values.stream().map(value -> value.type() + ":" + value.targetId()).distinct().count();
		if (distinct != values.size()) throw new BusinessRuleViolationException("DUPLICATE_SCOPE", "O mesmo escopo foi informado mais de uma vez.");
		values.forEach(value -> {
			boolean exists = switch (value.type()) {
				case UNIT -> organization.unitExists(organizationId(), value.targetId());
				case SECTOR -> organization.sectorExists(organizationId(), value.targetId());
				case EMPLOYEE -> employees.findScope(organizationId(), value.targetId()).isPresent();
			};
			if (!exists) throw new ResourceNotFoundException("O alvo do escopo informado não existe na organização.");
		});
	}

	private void validateEmployeeLink(UserRole role, UUID employeeId) {
		if (role == UserRole.EMPLOYEE && employeeId == null) {
			throw new BusinessRuleViolationException("EMPLOYEE_LINK_REQUIRED", "O perfil EMPLOYEE exige um colaborador vinculado.");
		}
		if (employeeId != null) employees.findScope(organizationId(), employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));
	}

	private void ensureEmailAvailable(String email, UUID currentId) {
		users.findByEmailIgnoreCase(User.normalizeEmail(email)).filter(user -> !user.getId().equals(currentId)).ifPresent(user -> {
			throw new ResourceConflictException("EMAIL_ALREADY_EXISTS", "E-mail já utilizado.");
		});
	}

	private User find(UUID id) {
		return users.findByIdAndOrganizationId(id, organizationId())
				.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
	}

	private UserResponse response(User user) {
		Set<Permission> effective = user.getRole() == UserRole.ADMIN ? EnumSet.allOf(Permission.class)
				: permissions.findAllByUserId(user.getId()).stream().map(UserPermission::getPermission).collect(Collectors.toUnmodifiableSet());
		return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getEmployeeId(),
				effective, user.getCreatedAt(), user.getUpdatedAt());
	}

	private void audit(String action, User target, Map<String, String> details) {
		var actor = currentUserProvider.requireCurrentUser();
		audit.record(new AuditRecord(actor.organizationId(), actor.userId(), action, "USER", target.getId(),
				clock.instant(), details));
	}

	private UUID organizationId() { return currentUserProvider.requireCurrentUser().organizationId(); }
}
