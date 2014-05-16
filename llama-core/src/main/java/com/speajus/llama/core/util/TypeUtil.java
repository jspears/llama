package com.speajus.llama.core.util;

import org.springframework.core.MethodParameter;

public final class TypeUtil {

	private TypeUtil() {
	}

	public static boolean canAssign(Object o, Class<?> clazz) {
		if (o == null) {
			return true;
		}
		return clazz.isAssignableFrom(o.getClass());
	}

	public static boolean canAssign(Object o, MethodParameter mp) {
		return canAssign(o, mp.getParameterType());
	}

	public static boolean canAssignNullNotOK(Object o, Class<?> clazz) {
		if (o == null) {
			return false;
		}
		return canAssign(o, clazz);
	}
}
