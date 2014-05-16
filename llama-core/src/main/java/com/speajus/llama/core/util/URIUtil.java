package com.speajus.llama.core.util;

import static java.net.URLDecoder.decode;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedHashMap;

import com.speajus.llama.core.LlamaException;

public class URIUtil {
	public static LinkedHashMap<String, String> query(final URI uri) {
		final LinkedHashMap<String, String> pairsHashMap = new LinkedHashMap<String, String>();
		if (uri == null || uri.getQuery() == null) {
			return pairsHashMap;
		}
		for (final String pair : uri.getQuery().split("&")) {
			final int idx = pair.indexOf("=");
			try {
				pairsHashMap.put(decode(pair.substring(0, idx), "UTF-8"),
						decode(pair.substring(idx + 1), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new LlamaException(e);
			}
		}
		return pairsHashMap;
	}
}
