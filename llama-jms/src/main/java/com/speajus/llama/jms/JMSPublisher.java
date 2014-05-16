package com.speajus.llama.jms;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.util.StringUtils;

import com.speajus.llama.annotation.Header;
import com.speajus.llama.core.LlamaException;
import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.core.ParameterResolver;
import com.speajus.llama.core.Publisher;
import com.speajus.llama.core.util.HeaderUtil;
import com.speajus.llama.core.util.TypeUtil;
import com.speajus.llama.core.util.URIUtil;

public class JMSPublisher implements Publisher, ApplicationContextAware {
	private static final Logger LOG = LoggerFactory
			.getLogger(JMSPublisher.class);
	protected ApplicationContext context;

	@LlamaQualifier
	@Autowired
	JmsTemplate template;

	final boolean pubSubDomain;

	public JMSPublisher() {
		this(false);
	}

	public JMSPublisher(boolean usePubSub) {
		this.pubSubDomain = usePubSub;
	}

	@Autowired
	MessageConverter converter;

	@LlamaQualifier
	@Autowired
	ConversionService conversionService;

	@Value("${llama.jms.publish.timeout:5000}")
	Long timeout;

	protected long timeout(Map<String, String> map) {
		String timeoutStr = map.get("timeout");
		if (StringUtils.isEmpty(timeoutStr))
			return timeout;
		return Long.parseLong(timeoutStr);
	}

	@Override
	public Object publish(final URI uri, final Object[] args,
			final Method method) {

		final Map<String, String> query = URIUtil.query(uri);

		template.setPubSubDomain(pubSubDomain);

		final Class<?> ret = method.getReturnType();

		final Object retObj;

		if (ret == void.class || ret == Void.class) {
			retObj = null;
			template.send(uri.getHost(), createCreator(method, args));
		} else {

			LOG.warn("making sync call to {} replyTo: {} ", uri.getHost(),
					query.get("replyTo"));

			final Message retMesg = template.execute(
					new SyncProducerCallback(uri.getHost(), query
							.get("replyTo"), timeout(query), pubSubDomain,
							createCreator(method, args), template
									.getDestinationResolver()), true);
			if (TypeUtil.canAssign(retMesg, ret)) {
				retObj = retMesg;
			} else {
				try {
					final Object result = this.converter.fromMessage(retMesg);
					if (TypeUtil.canAssign(result, ret)) {
						retObj = result;
					} else {
						retObj = this.conversionService.convert(result, ret);
					}
				} catch (final MessageConversionException | JMSException e) {
					throw new LlamaException(e);
				}

			}
		}

		return retObj;
	}

	protected MessageCreator createCreator(final Method method,
			final Object... args) {
		final Collection<MethodParameter> parameters = ParameterResolver
				.parameters(method);
		return new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				final Map<String, Object> headers = HeaderUtil
						.headersFrom(method);
				Object body = null;
				for (MethodParameter mp : parameters) {
					Header h = mp.getParameterAnnotation(Header.class);
					if (h == null) {
						body = args[mp.getParameterIndex()];
					} else {
						headers.put(h.value(), args[mp.getParameterIndex()]);
					}
				}
				final Message m;
				if (body == null) {
					m = session.createMessage();
				} else {
					m = converter.toMessage(body, session);
				}
				for (final Map.Entry<String, Object> entry : headers.entrySet()) {
					m.setObjectProperty(entry.getKey(), entry.getValue());
				}
				return m;
			}

		};
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0)
			throws BeansException {
		this.context = arg0;

	}
}
