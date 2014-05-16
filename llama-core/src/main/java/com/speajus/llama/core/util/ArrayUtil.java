package com.speajus.llama.core.util;

public final class ArrayUtil {
	private static final Object[] EMPTY = {};

	private ArrayUtil() {
	}

	@SafeVarargs
	public static <T> T[] array(final T... arr) {
		return arr;
	}

	@SafeVarargs
	public static Object[] objects(final Object... arr) {
		return arr == null ? EMPTY : arr;
	}
}
