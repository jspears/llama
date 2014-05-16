package com.speajus.llama.service;

import java.net.URI;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import com.speajus.llama.core.LlamaException;
import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.jms.JMSPublisher;
import com.speajus.llama.jms.JMSSubscriber;

@Configuration
@Import(LlamaContext.class)
public class JMSContext {

	@Autowired
	ConnectionFactory connectionFactory;

	@Autowired(required = false)
	JmsTemplate template;

	@Autowired(required = false)
	MessageConverter messageConverter;

	@Bean
	@LlamaQualifier
	JmsTemplate llamaJmsTemplate() {
		if (template == null) {
			template = new JmsTemplate();
			template.setConnectionFactory(connectionFactory);
		}
		return template;
	}

	@Bean
	JMSPublisher topicPublisher() {
		return new JMSPublisher(true);
	}

	@Bean
	JMSPublisher queuePublisher() {
		return new JMSPublisher();
	}

	@Bean
	JMSSubscriber topicSubscriber() {
		return new JMSSubscriber(true);
	}

	@Bean
	JMSSubscriber queueSubscriber() {
		return new JMSSubscriber();
	}

	@Bean(name = "llamaJMSListenerContainer", initMethod = "start", destroyMethod = "destroy")
	@Scope("prototype")
	DefaultMessageListenerContainer llamaJMSListenerContainer(final URI uri,
			final boolean pubSubDomain,
			final SessionAwareMessageListener<?> messageListener) {
		DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
		dmlc.setConnectionFactory(connectionFactory);
		dmlc.setAutoStartup(true);
		dmlc.setBeanName(uri.toString());
		dmlc.setPubSubDomain(pubSubDomain);
		final String destination = uri.getHost();
		dmlc.setDestinationName(destination);
		dmlc.setMessageListener(messageListener);
		dmlc.afterPropertiesSet();
		dmlc.start();
		return dmlc;
	}

	@Bean
	MessageConverter messageConverter() {
		if (messageConverter != null) {
			return this.messageConverter;
		}
		return (messageConverter = new SimpleMessageConverter());
	}

	@Bean
	Converter<TextMessage, String> textMessage() {
		return new Converter<TextMessage, String>() {

			@Override
			public String convert(TextMessage arg0) {
				try {
					return arg0.getText();
				} catch (JMSException e) {
					throw new LlamaException(e);
				}
			}
		};
	}

	@Bean
	Converter<Message, String> message() {
		return new Converter<Message, String>() {

			@Override
			public String convert(Message arg0) {
				try {
					Object o = messageConverter.fromMessage(arg0);
					return (String) o;
				} catch (MessageConversionException | JMSException e) {
					throw new LlamaException(e);
				}
			}
		};
	}
}
