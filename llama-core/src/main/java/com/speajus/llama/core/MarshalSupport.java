package com.speajus.llama.core;

import static com.speajus.llama.core.util.TypeUtil.canAssign;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import com.speajus.llama.annotation.Body;
import com.speajus.llama.annotation.Header;

@Component
public class MarshalSupport {
	@Autowired
	@LlamaQualifier
	ConversionService conversionService;

	@Autowired
	ApplicationContext context;

	public Object convertTo(Object source, Class<?> targetType) {
		if (source == null || canAssign(source, targetType)) {
			return source;
		}
		return conversionService.convert(source, targetType);
	}

	public Object marshalOrConverTo(Object value, MethodParameter mp) {
		if (value == null || canAssign(value, mp)) {
			return value;
		}
		final Object ret = unmarshal(mp, value);
		if (ret == null) {
			return convertTo(value, mp.getParameterType());
		}
		return ret;
	}

	protected String extractConverter(final MethodParameter mp) {
		Body m = mp.getParameterAnnotation(Body.class);
		final String converter;
		if (m != null) {
			converter = m.converter();
		} else {
			Header h = mp.getParameterAnnotation(Header.class);
			if (h != null) {
				converter = h.converter();
			} else {
				converter = null;
			}
		}
		return converter;
	}

	public Object marshal(MethodParameter mp, Object value) {
		return marshal(extractConverter(mp), value, mp.getParameterType());
	}

	public Object marshal(String marshallerName, Object value, Class<?> target) {
		final Marshaller um = (Marshaller) context.getBean(marshallerName,
				target);

		Result r;
		if (target.isAssignableFrom(String.class)) {
			StringWriter sw = new StringWriter();
			r = new StreamResult(sw);
			try {
				um.marshal(value, r);
			} catch (XmlMappingException | IOException e) {
				throw new LlamaException(e);
			}
			return sw.toString();
		}

		return null;
	}

	public Object unmarshal(MethodParameter mp, Object value) {

		return unmarshal(extractConverter(mp), value, mp.getParameterType());
	}

	@SuppressWarnings("unchecked")
	public <T> T unmarshal(final String marshallerName, final Object value,
			final Class<T> target) {
		if (canAssign(value, target)) {
			return (T) value;
		}
		final Class<?> valueType = value.getClass();
		final Unmarshaller um = (Unmarshaller) context.getBean(marshallerName,
				target);
		final Source source;
		SOURCE: {
			if (value instanceof String) {
				source = new StreamSource(new StringReader((String) value));
				break SOURCE;
			}
			if (value instanceof Reader) {
				source = new StreamSource((Reader) value);
				break SOURCE;
			}
			if (value instanceof InputStream) {
				source = new StreamSource((InputStream) value);
				break SOURCE;
			}
			if (value instanceof Node) {
				source = new DOMSource((Node) value);
				break SOURCE;
			}
			if (this.conversionService.canConvert(valueType, Source.class)) {
				source = this.conversionService.convert(value, Source.class);
				break SOURCE;
			}
			if (this.conversionService.canConvert(valueType, InputStream.class)) {
				source = new StreamSource(this.conversionService.convert(value,
						InputStream.class));
				break SOURCE;
			}
			throw new LlamaException(
					"unable to marshal from "
							+ valueType
							+ " to supported marshalling type string, Reader, InputStream, Node, Source");
		}
		try {
			return um.unmarshal(source, target).getValue();
		} catch (JAXBException e) {
			throw new LlamaException(e);
		}
	}
}
