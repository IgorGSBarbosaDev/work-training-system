package dev.igorbarbosa.worktrainingsystem.identity.application;

public interface PasswordResetDeliveryPort {
	void deliver(String email, String opaqueToken);
}
