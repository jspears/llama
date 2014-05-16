package com.speajus.llama.core.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import com.speajus.llama.annotation.Header;
import com.speajus.llama.annotation.Headers;

public final class HeaderUtil {
	private HeaderUtil() {
	}

	public static Map<String, Object> headersFrom(Method m) {
		Map<String, Object> headers = new HashMap<>();
		{
			Header h = findAnnotation(m, Header.class);
			if (h != null) {
				headers.put(h.key(), h.value());
			}
		}

		Headers hs = findAnnotation(m, Headers.class);
		if (hs != null && hs.value() != null) {
			for (Header h : hs.value()) {
				headers.put(h.key(), h.value());

			}
		}
		return headers;
	}

	@SafeVarargs
	public static <K, V> Map<K, V> merge(Map<K, V> map, Map<K, V>... maps) {
		if (maps != null && maps.length > 0) {
			for (Map<K, V> m : maps)
				map.putAll(m);
		}

		return map;
	}
}
