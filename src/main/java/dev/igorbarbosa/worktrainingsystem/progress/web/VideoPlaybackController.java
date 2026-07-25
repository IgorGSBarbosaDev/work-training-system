package dev.igorbarbosa.worktrainingsystem.progress.web;

import dev.igorbarbosa.worktrainingsystem.progress.api.PlaybackUrlResponse;
import dev.igorbarbosa.worktrainingsystem.progress.application.VideoPlaybackService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoPlaybackController {
	private final VideoPlaybackService service;
	public VideoPlaybackController(VideoPlaybackService service) { this.service = service; }
	@PostMapping("/{videoId}/playback-url") @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
	public PlaybackUrlResponse playback(@PathVariable UUID videoId) { return service.playbackUrl(videoId); }
}
