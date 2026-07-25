package dev.igorbarbosa.worktrainingsystem.identity.web;

import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.AuthTokens;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ChangePasswordRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ForgotPasswordRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.LoginRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.LogoutRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.RefreshRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ResetPasswordRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UserResponse;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthenticationService authentication;
	public AuthController(AuthenticationService authentication) { this.authentication = authentication; }

	@PostMapping("/login")
	public AuthTokens login(@Valid @RequestBody LoginRequest request) { return authentication.login(request); }
	@PostMapping("/refresh")
	public AuthTokens refresh(@Valid @RequestBody RefreshRequest request) { return authentication.refresh(request.refreshToken()); }
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authentication.logout(request.refreshToken()); return ResponseEntity.noContent().build();
	}
	@GetMapping("/me")
	public UserResponse me() { return authentication.me(); }
	@PostMapping("/password/forgot")
	public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
		authentication.forgotPassword(request.email()); return ResponseEntity.accepted().build();
	}
	@PostMapping("/password/reset")
	public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
		authentication.resetPassword(request); return ResponseEntity.noContent().build();
	}
	@PatchMapping("/password")
	public ResponseEntity<Void> change(@Valid @RequestBody ChangePasswordRequest request) {
		authentication.changePassword(request); return ResponseEntity.noContent().build();
	}
}
