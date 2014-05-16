package com.speajus.llama.file;

import static com.speajus.llama.core.util.ArrayUtil.array;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringUtils;

import com.speajus.llama.core.LlamaException;
import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.core.ParameterResolver;
import com.speajus.llama.core.ParameterResolver.Invoker;
import com.speajus.llama.core.Subscriber;
import com.speajus.llama.core.util.URIUtil;

public class FileSubscriber implements Subscriber {
	private static final Logger LOG = LoggerFactory
			.getLogger(FileSubscriber.class);

	private static final WatchEvent.Kind<Path>[] KINDS = array(
			StandardWatchEventKinds.ENTRY_CREATE,
			StandardWatchEventKinds.ENTRY_DELETE,
			StandardWatchEventKinds.ENTRY_MODIFY);

	private static final TriggerStrategy NOOP_TRIGGER_STRATEGY = new TriggerStrategy() {

		@Override
		public Path resolve(Path path) {
			return path;
		}
	};

	@Value("${llama.file.pollTime:100}")
	protected long pollTime;

	@Value("${llama.file.pollTimeUnit:MILLISECONDS}")
	protected TimeUnit pollTimeUnit;

	@Autowired
	@LlamaQualifier
	protected ConversionService conversionService;

	@Autowired
	@LlamaQualifier
	protected ScheduledExecutorService scheduled;

	@Autowired
	protected ParameterResolver resolver;

	@Autowired
	protected ApplicationContext factory;

	protected final TriggerStrategy triggerStrategy;

	public FileSubscriber() {
		this(NOOP_TRIGGER_STRATEGY);
	}

	public FileSubscriber(TriggerStrategy strategy) {
		this.triggerStrategy = strategy;
	}

	public TriggerStrategy resolveTrigger(String extension) {
		if (StringUtils.isEmpty(extension)) {
			return triggerStrategy;
		}
		return (TriggerStrategy) factory.getBean("extensionTriggerStrategy",
				extension);
	}

	protected File createFile(URI uri) {
		File f = new File(uri.getSchemeSpecificPart()
				.replaceFirst("\\/\\/", "").replaceFirst("\\?.*", ""));
		f.mkdirs();
		LOG.info("monitoring directory {}", f.getAbsoluteFile());
		return f;
	}

	@Override
	public void register(URI uri, Method method, Object bean, String beanName) {
		final File f = createFile(uri);

		final Path p = f.getAbsoluteFile().toPath();

		final Map<String, String> map = URIUtil.query(uri);
		final Invoker invoker = resolver.createInvoker(method, bean);
		final WatchDirectory wd;
		try {
			wd = new WatchDirectory(p,
					resolveTrigger(map.get("triggerExtension")),
					new WatchDirectory.FileEventHandler() {

						@Override
						public void onEvent(final Path path, final Kind<?> kind) {
							LOG.debug("event: {} {}", path, kind);
							scheduled.execute(new Runnable() {
								public void run() {
									try {
										invoker.invoke(attrsAsMap(path), path,
												kind);
										Files.delete(path);
									} catch (Throwable e) {
										LOG.warn("Caught error on {}", path, e);
									}
								}
							});
						}
					}, findKinds(map));
		} catch (IOException e) {
			throw new LlamaException(e);
		}
		scheduled.scheduleAtFixedRate(wd, 10, pollTime, pollTimeUnit);
	}

	protected Map<String, Object> attrsAsMap(final Path path) {
		final Map<String, Object> map = new HashMap<>();
		try {
			BasicFileAttributes attr = Files.readAttributes(path,
					BasicFileAttributes.class);
			map.put("creationTime", attr.creationTime().toMillis());
			map.put("isDirectory", attr.isDirectory());
			map.put("isRegularFile", attr.isRegularFile());
			map.put("isSymbolicLink", attr.isSymbolicLink());
			map.put("isOther", attr.isOther());
			map.put("lastAccessTime", attr.lastAccessTime().toMillis());
			map.put("lastModifiedTime", attr.lastModifiedTime().toMillis());
			map.put("size", attr.size());
			map.put("fileName", path.getFileName().toString());
		} catch (IOException e) {
			LOG.warn("could not get attributes for {}", path, e);
		}
		return map;
	}

	protected WatchEvent.Kind<?>[] findKinds(Map<String, String> map) {
		final String eventType = map.get("watchEvent");
		if (StringUtils.isEmpty(eventType)) {
			return array(StandardWatchEventKinds.ENTRY_CREATE);
		} else {
			final Set<String> types = new HashSet<>(Arrays.asList(eventType
					.split(",")));
			final Set<WatchEvent.Kind<Path>> ret = new HashSet<>();
			for (WatchEvent.Kind<Path> kind : KINDS) {
				if (types.contains(kind.name())) {
					ret.add(kind);
				}
			}
			return ret.toArray(KINDS);
		}

	}
}
