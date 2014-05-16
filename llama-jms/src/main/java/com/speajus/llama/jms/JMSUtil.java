package com.speajus.llama.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.springframework.util.CollectionUtils.toArray;

public final class JMSUtil {
	private static final String[] EMPTY = {};

	private JMSUtil() {
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> headers(final Message mesg)
			throws JMSException {

		final Map<String, Object> map = new HashMap<>();
		for (final String key : toArray(mesg.getPropertyNames(), EMPTY)) {
			map.put(key, mesg.getObjectProperty(key));

		}
		return map;
	}
}
