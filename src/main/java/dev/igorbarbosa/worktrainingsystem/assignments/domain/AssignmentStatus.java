package dev.igorbarbosa.worktrainingsystem.assignments.domain;

public enum AssignmentStatus {
	NOT_STARTED,
	IN_PROGRESS,
	AWAITING_ASSESSMENT,
	APPROVED,
	FAILED,
	COMPLETED,
	EXPIRING_SOON,
	EXPIRED,
	CANCELLED,
	WAIVED;

	public boolean canCloseAdministratively() {
		return this == NOT_STARTED || this == IN_PROGRESS || this == AWAITING_ASSESSMENT;
	}

	public boolean isTerminal() {
		return this == COMPLETED || this == EXPIRING_SOON || this == EXPIRED
				|| this == CANCELLED || this == WAIVED;
	}
}
