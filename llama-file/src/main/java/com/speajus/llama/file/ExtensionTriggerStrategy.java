package com.speajus.llama.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionTriggerStrategy implements TriggerStrategy {
	private static final Logger LOG = LoggerFactory
			.getLogger(ExtensionTriggerStrategy.class);

	final String extension;

	final boolean delete;

	final boolean strictOrder;

	public ExtensionTriggerStrategy(String extension) {
		this(extension, true, false);
	}

	public ExtensionTriggerStrategy(String extension, boolean delete,
			boolean strictOrder) {
		this.extension = extension;
		this.delete = delete;
		this.strictOrder = strictOrder;
	}

	@Override
	public Path resolve(Path path) {
		final String fileName = path.getFileName().toString();
		if (fileName.endsWith(extension)) {

			final int length = fileName.length();
			final Path realPath = path.resolveSibling(fileName.substring(0,
					length - extension.length()));
			try {
				if (strictOrder) {
					Files.delete(path);
				} else {
					if (Files.exists(realPath)) {
						Files.delete(path);
					} else {
						return null;
					}
				}

			} catch (IOException e) {
				LOG.warn("could not delete trigger file {}", path, e);
			}
			return realPath;
		} else if (!strictOrder) {
			final Path outPath = path.resolveSibling(fileName + extension);
			if (Files.exists(outPath)) {
				LOG.info("recieved trigger before file {}", fileName);
				return resolve(outPath);
			}
		}
		return null;
	}

}
