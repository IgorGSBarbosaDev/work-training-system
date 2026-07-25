package dev.igorbarbosa.worktrainingsystem.files.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UploadCleanupJob {
	private final FileService files;
	public UploadCleanupJob(FileService files) { this.files = files; }
	@Scheduled(fixedDelayString = "${app.uploads.cleanup-delay:5m}")
	public void expirePendingUploads() { files.expirePending(); }
}
