package com.speajus.llama.jms;

import java.lang.reflect.Method;
import java.net.URI;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;

import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.core.ParameterResolver;
import com.speajus.llama.core.ParameterResolver.Invoker;
import com.speajus.llama.core.Subscriber;

public class JMSSubscriber implements Subscriber, ApplicationContextAware {
	private static final Logger LOG = LoggerFactory
			.getLogger(JMSSubscriber.class);
	private ApplicationContext context;
	final boolean pubSubDomain;

	@Autowired
	ParameterResolver resolver;

	@Autowired
	MessageConverter converter;

	@LlamaQualifier
	@Autowired
	JmsTemplate template;

	public JMSSubscriber() {
		this(false);
	}

	public JMSSubscriber(boolean pubSub) {
		this.pubSubDomain = pubSub;
	}

	@Override
	public void register(final URI uri, final Method method, final Object bean,
			final String beanName) {
		final Invoker invoker = resolver.createInvoker(method, bean);
		createContainer(uri, new SessionAwareMessageListener<Message>() {

			@Override
			public void onMessage(Message message, Session session)
					throws JMSException {
				LOG.debug("recieved message {}", uri);

				Object o = invoker.invoke(JMSUtil.headers(message), message,
						converter.fromMessage(message),
						message.getJMSDestination());
				final Destination replyTo = message.getJMSReplyTo();
				if (replyTo != null) {
					LOG.debug(
							"replying to {} replyTo: {} messageId: {} correlationId: {}",
							message.getJMSDestination(), replyTo,
							message.getJMSMessageID(),
							message.getJMSCorrelationID());

					final Message m = o == null ? session.createTextMessage()
							: converter.toMessage(o, session);

					m.setJMSCorrelationID(message.getJMSCorrelationID());

					MessageProducer producer = null;
					try {

						producer = session.createProducer(replyTo);
						producer.send(m);
						LOG.debug("sent message to {}", replyTo);
					} finally {
						JmsUtils.closeMessageProducer(producer);
					}
				}
			}
		});
	}

	protected void createContainer(URI uri,
			SessionAwareMessageListener<Message> m) {
		context.getBean("llamaJMSListenerContainer", uri, this.pubSubDomain, m);
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0)
			throws BeansException {
		this.context = arg0;

	}

}
