package com.speajus.llama.file;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchDirectory implements Runnable {
	private static final Logger LOG = LoggerFactory
			.getLogger(WatchDirectory.class);
	final Path directory;
	final WatchService watcher;
	final FileEventHandler handler;
	final TriggerStrategy triggerStrategy;

	public WatchDirectory(Path directory, TriggerStrategy triggerStrategy,
			FileEventHandler handler, Kind<?>... kinds) throws IOException {
		super();
		this.directory = directory;
		this.handler = handler;
		this.triggerStrategy = triggerStrategy;
		this.watcher = directory.getFileSystem().newWatchService();
		this.directory.register(watcher, kinds, attemptSunModifier());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Modifier attemptSunModifier() {
		Class<Enum> mod;
		try {
			mod = (Class<Enum>) Class.forName(
					"com.sun.nio.file.SensitivityWatchEventModifier", true,
					Thread.currentThread().getContextClassLoader());
			Modifier m = (Modifier) Enum.valueOf(mod, "HIGH");
			return m;
		} catch (ClassNotFoundException e) {
			LOG.info("could not set the sensitivity level ", e);
		}
		return null;

	}

	public interface FileEventHandler {
		public void onEvent(Path path, WatchEvent.Kind<?> kind);
	}

	public void run() {

		// poll for the key.
		final WatchKey key = watcher.poll();
		if (key == null) {
			return;
		}

		for (final WatchEvent<?> event : key.pollEvents()) {
			final WatchEvent.Kind<?> kind = event.kind();

			// This key is registered only
			// for ENTRY_CREATE events,
			// but an OVERFLOW event can
			// occur regardless if events
			// are lost or discarded.
			if (kind == OVERFLOW) {
				LOG.warn("overflow? {}", kind);
				continue;
			}
			// The filename is the
			// context of the event.
			@SuppressWarnings("unchecked")
			final WatchEvent<Path> ev = (WatchEvent<Path>) event;
			final Path filename = ev.context();
			final Path child = triggerStrategy.resolve(directory
					.resolve(filename));
			if (child != null) {
				handler.onEvent(child, kind);
			}
		}

		// Reset the key -- this step is critical if you want to
		// receive further watch events. If the key is no longer valid,
		// the directory is inaccessible so exit the loop.
		if (!key.reset()) {
			key.cancel();
			LOG.info("no longer valid key {}", key);
			try {
				watcher.close();
			} catch (IOException e) {
				LOG.warn("error closing watchService {}", key, e);
			}
			return;
		}
	}

}
