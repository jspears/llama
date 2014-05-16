package com.speajus.llama.core;

import java.lang.reflect.Method;
import java.net.URI;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.StringValueResolver;

import com.speajus.llama.annotation.Subscribe;

@Component
public class SubscribeBeanPostProcessor implements BeanPostProcessor,
		ApplicationContextAware, EmbeddedValueResolverAware {

	private ApplicationContext context;
	private StringValueResolver valueResolver;

	@Override
	public Object postProcessAfterInitialization(Object arg0, String arg1)
			throws BeansException {
		return arg0;
	}

	@Override
	public Object postProcessBeforeInitialization(final Object bean,
			final String beanName) throws BeansException {

		ReflectionUtils.doWithMethods(bean.getClass(), new MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException,
					IllegalAccessException {
				final Subscribe subscribe = AnnotationUtils.findAnnotation(
						method, Subscribe.class);
				if (subscribe != null) {
					final URI uri = URI.create(valueResolver
							.resolveStringValue(subscribe.value()));
					final Subscriber sr = context.getBean(uri.getScheme()+"Subscriber",
							Subscriber.class);
					sr.register(uri, method, bean, beanName);
				}

			}
		});
		return bean;
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0)
			throws BeansException {
		this.context = arg0;

	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver arg0) {
		this.valueResolver = arg0;

	}

}
