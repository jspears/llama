package com.speajus.llama.jms;

import java.util.UUID;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.DestinationResolver;

public class SyncProducerCallback implements SessionCallback<Message> {
	private static final Logger LOG = LoggerFactory
			.getLogger(SyncProducerCallback.class);
	String destinationName;
	Destination replyTo;
	String replyToName;
	long timeout;
	MessageCreator creator;
	DestinationResolver destinationResolver;
	boolean pubSubDomain = false;

	public SyncProducerCallback(String destinationName, String replyTo,
			long timeout, boolean pubSubDomain, MessageCreator creator,
			DestinationResolver destinationResolver) {
		super();
		this.destinationName = destinationName;
		this.replyToName = replyTo;
		this.timeout = timeout;
		this.creator = creator;
		this.pubSubDomain = pubSubDomain;
		this.destinationResolver = destinationResolver;
	}

	Destination resolveReplyTo(Session session) throws JMSException {
		if (replyTo != null) {
			return replyTo;
		}
		if (replyToName != null) {
			replyTo = destinationResolver.resolveDestinationName(session,
					replyToName, pubSubDomain);
		} else {
			replyTo = pubSubDomain ? session.createTemporaryTopic() : session
					.createTemporaryQueue();
		}
		return replyTo;
	}

	@Override
	public Message doInJms(Session session) throws JMSException {
		final String correlationId = UUID.randomUUID().toString();
		MessageConsumer consumer = null;
		MessageProducer producer = null;
		final Destination requestQueue = destinationResolver
				.resolveDestinationName(session, destinationName, pubSubDomain);
		final Destination replyQueue = resolveReplyTo(session);
		try {
			consumer = session.createConsumer(replyQueue,
					"JMSCorrelationID = '" + correlationId + "'");

			final Message message = creator.createMessage(session);
			message.setJMSCorrelationID(correlationId);
			message.setJMSReplyTo(replyQueue);

			producer = session.createProducer(destinationResolver
					.resolveDestinationName(session, destinationName,
							pubSubDomain));
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			// Send the request second!
			// producer = session.createProducer(requestQueue);
			LOG.debug("sending to {}", requestQueue);
			producer.send(requestQueue, message);
			// Block on receiving the response with a timeout

			Message ret = consumer.receive(timeout);
			return ret;
		} finally {
			JmsUtils.closeMessageConsumer(consumer);
			JmsUtils.closeMessageProducer(producer);
		}
	}
}
