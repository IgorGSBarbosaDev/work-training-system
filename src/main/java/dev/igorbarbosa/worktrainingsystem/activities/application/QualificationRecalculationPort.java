package dev.igorbarbosa.worktrainingsystem.activities.application;

public interface QualificationRecalculationPort {
	void recalculate(QualificationRecalculationRequested event);
}
