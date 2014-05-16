package com.speajus.llama.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.StringValueResolver;

import com.speajus.llama.annotation.Publish;

@Component
public class PublishBeanPostProcessor implements BeanPostProcessor,
		ApplicationContextAware, EmbeddedValueResolverAware {
	protected ClassLoader loader = Thread.currentThread()
			.getContextClassLoader();
	private ApplicationContext context;
	private StringValueResolver valueResolver;

	@Override
	public Object postProcessAfterInitialization(final Object bean,
			final String arg1) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(final Object bean,
			final String arg1) throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {

			@Override
			public void doWith(Field arg0) throws IllegalArgumentException,
					IllegalAccessException {
				final Publish pub = AnnotationUtils.getAnnotation(arg0,
						Publish.class);
				if (pub != null)
					assignField(arg0, pub, bean);
			}
		});
		ReflectionUtils.doWithMethods(bean.getClass(), new MethodCallback() {

			@Override
			public void doWith(Method arg0) throws IllegalArgumentException,
					IllegalAccessException {
				final Publish pub = AnnotationUtils.getAnnotation(arg0,
						Publish.class);
				if (pub != null)
					assignMethod(arg0, pub, bean);
			}

		});
		return bean;
	}

	protected void assignMethod(Method arg0, Publish pub, Object bean) {
		ReflectionUtils.invokeMethod(arg0, bean,
				createProxy(arg0.getParameterTypes()[0], pub, bean));

	}

	protected void assignField(Field field, Publish pub, Object bean) {
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		ReflectionUtils.setField(field, bean,
				createProxy(field.getType(), pub, bean));
	}

	protected Object createProxy(final Class<?> clz, final Publish pub,
			final Object bean) {
		return Proxy.newProxyInstance(loader, new Class[] { clz },
				new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						Publish methodPublish = AnnotationUtils.findAnnotation(
								method, Publish.class);
						String value = methodPublish == null ? pub.value()
								: methodPublish.value();
						URI uri = URI.create(valueResolver
								.resolveStringValue(value));
						return context
								.getBean(uri.getScheme()+"Publisher", Publisher.class)
								.publish(uri, args, method);
					}
				});
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx)
			throws BeansException {
		this.context = ctx;

	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver arg0) {
		this.valueResolver = arg0;

	}

}
