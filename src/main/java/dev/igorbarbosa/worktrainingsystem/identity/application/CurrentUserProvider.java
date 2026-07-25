package dev.igorbarbosa.worktrainingsystem.identity.application;

public interface CurrentUserProvider {
	CurrentUser requireCurrentUser();
}
