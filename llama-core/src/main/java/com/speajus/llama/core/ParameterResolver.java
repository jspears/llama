package com.speajus.llama.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.speajus.llama.annotation.Header;
import com.speajus.llama.core.util.TypeUtil;

public class ParameterResolver {

	@LlamaQualifier
	@Autowired
	ConversionService service;

	@Autowired
	MarshalSupport marshal;

	public ParameterResolver() {
	}

	public ParameterResolver(ConversionService service) {
		this.service = service;
	}

	public static List<MethodParameter> parameters(Method method) {
		final int size = method == null ? 0 : method.getParameterTypes().length;
		List<MethodParameter> parameters = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			parameters.add(MethodParameter.forMethodOrConstructor(method, i));
		}
		return parameters;

	}

	public Invoker createInvoker(Method m, Object b) {
		return new Invoker(marshal, m, b);
	}

	public static class Invoker {
		final MarshalSupport marshal;
		final Method method;
		final Object bean;
		final MethodParameter body;
		final Class<?> bodyType;
		final LinkedHashMap<MethodParameter, String> headerParameter;
		final int paramSize;

		/**
		 * Cache what we can.
		 * 
		 * @param m
		 * @param b
		 */
		Invoker(final MarshalSupport marshal, Method m, Object b) {
			this.marshal = marshal;
			this.method = m;
			this.bean = b;
			List<MethodParameter> parameters = parameters(m);
			paramSize = parameters.size();
			MethodParameter tmpBody = null;
			this.headerParameter = new LinkedHashMap<>();

			for (MethodParameter mp : parameters) {
				Header h = mp.getParameterAnnotation(Header.class);
				if (h == null) {
					tmpBody = mp;
				} else {
					headerParameter.put(mp, h.value());
				}
			}

			this.body = tmpBody;

			this.bodyType = body == null ? null : body.getParameterType();

		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Invoker))
				return false;
			if (obj == this)
				return true;
			Invoker that = (Invoker) obj;
			return (that.method.equals(method) && that.bean.equals(bean));

		}

		@Override
		public int hashCode() {
			return 37 * bean.hashCode() + method.hashCode();
		}

		public Object invoke(Map<String, ?> headers, Object bodyContent,
				Object... resolvables) {
			Object bodyVal = null;
			final Object[] args = new Object[paramSize];

			if (body != null) {
				NEEDS_BODY: {

					if (resolvables != null) {
						for (Object resolve : resolvables) {
							if (TypeUtil.canAssign(resolve, bodyType)) {
								bodyVal = resolve;
								break NEEDS_BODY;
							}
						}
					}
					bodyVal = marshal.marshalOrConverTo(bodyContent, body);
					break NEEDS_BODY;
				}
				args[body.getParameterIndex()] = bodyVal;
			}
			for (final Entry<MethodParameter, String> mp : headerParameter
					.entrySet()) {
				final MethodParameter key = mp.getKey();
				final String value = mp.getValue();
				final int idx = key.getParameterIndex();
				if (StringUtils.isEmpty(value)) {
					if (TypeUtil.canAssign(headers, key.getParameterType())) {
						args[idx] = headers;
						continue;
					}
					throw new LlamaException(
							"param type is not assignable to map and has empty value "
									+ key);

				}
				final Object ho = headers.get(value);
				args[idx] = marshal.convertTo(ho, key.getParameterType());

			}
			return ReflectionUtils.invokeMethod(method, bean, args);
		}
	}
}
