package dev.igorbarbosa.worktrainingsystem.shared.storage.application;

import java.io.IOException;
import java.io.InputStream;

public record StoredObject(InputStream content, long contentLength, String contentType) implements AutoCloseable {

	@Override
	public void close() throws IOException {
		content.close();
	}
}
