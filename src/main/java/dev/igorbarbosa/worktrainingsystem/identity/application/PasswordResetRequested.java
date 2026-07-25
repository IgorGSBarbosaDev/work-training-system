package dev.igorbarbosa.worktrainingsystem.identity.application;

record PasswordResetRequested(String email, String opaqueToken) {
}
