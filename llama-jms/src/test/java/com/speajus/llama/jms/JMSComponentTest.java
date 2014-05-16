package com.speajus.llama.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.speajus.llama.annotation.Header;
import com.speajus.llama.annotation.Headers;
import com.speajus.llama.annotation.Publish;
import com.speajus.llama.annotation.Subscribe;
import com.speajus.llama.jms.JMSComponentTest.LikeThings;
import com.speajus.llama.service.JMSContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { JMSTestContext.class, JMSContext.class })
public class JMSComponentTest {
	public interface Sender {
		void topic(String name, @Header("value") String value);

		@Publish("queue://things.I.dunnoabout")
		@Header(key = "test", value = "me")
		@Headers(@Header(key = "what", value = "worries"))
		void queue(String mesg);

		@Publish("queue://things.I.testme")
		TextMessage testMe(String words);

		@Publish("queue://things.I.testme")
		String testStr(String words);

		@Publish("queue://things.I.testme")
		InputStream testInput(String words);
	}
	

	@Autowired
	LikeThings things;

	@Publish("topic://things.I.like")
	Sender sender;

	@Test
	public void seender_async() {
		TextMessage m = sender.testMe("there");
		assertNotNull(m);
	}

	@Test
	public void seender_async_convert() {
		String m = sender.testStr("there");
		assertNotNull(m);
		assertEquals("Hello, there!", m);
	}
	@Test
	public void seender_async_convertInputStr() {
		InputStream m = sender.testInput("there");
		assertNotNull(m);
		assertTrue(m instanceof ByteArrayInputStream);
	}

	@Test
	public void senderTopic_withHeader_waitsForMessage()
			throws InterruptedException {
		sender.topic("hello", "goodbye");
		assertTrue(things.onLike.await(10, TimeUnit.SECONDS));
		assertEquals("hello", things.mesg);
		assertEquals("goodbye", things.value);

	}

	@Test
	public void senderQueue_withHeader_waitsForMessage()
			throws InterruptedException {
		sender.queue("metoo");
		assertTrue(things.onDunno.await(10, TimeUnit.SECONDS));
		assertEquals("metoo", things.dunno);
		assertEquals("me", things.test);
		assertEquals("worries", things.what);

	}

	public static class LikeThings {
		CountDownLatch onLike = new CountDownLatch(1);
		CountDownLatch onDunno = new CountDownLatch(1);

		String mesg;
		String value;
		String dunno;
		String test;
		String what;

		@Subscribe("topic://things.I.like")
		public void onLike(String mesg, @Header("value") String value) {
			this.mesg = mesg;
			this.value = value;
			onLike.countDown();

		}

		@Subscribe("queue://things.I.dunnoabout")
		public void onDunno(@Header("what") String what,
				@Header("test") String test, String mesg) {
			this.dunno = mesg;
			this.what = what;
			this.test = test;
			onDunno.countDown();
		}

		@Subscribe("queue://things.I.testme")
		public String queue(String body) {
			return "Hello, " + body + "!";
		}
	}
}

@Configuration
class JMSTestContext {

	@Bean
	LikeThings ilike() {
		return new LikeThings();
	}

	@Bean
	Broker broker() throws Exception {
		BrokerService broker = new BrokerService();
		broker.setBrokerId("test-broker");
		broker.setPersistent(false);
		broker.setBrokerName("localhost");
		broker.setUseJmx(false);
		broker.setSchedulerSupport(true);
		broker.start();
		return broker.getBroker();
	}

	@Bean
	ConnectionFactory connectionFactory(Broker broker) {
		ActiveMQConnectionFactory acf = new ActiveMQConnectionFactory();
		acf.setBrokerURL(broker.getVmConnectorURI().toString());
		return acf;
	}

}
