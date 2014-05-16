package com.speajus.llama.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.speajus.llama.annotation.Body;
import com.speajus.llama.annotation.Publish;
import com.speajus.llama.core.SubscribeBeanPostProcessorTest.SubTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PublishBeanPostProcessorContext.class)
public class PublishBeanPostProcessorTest {
	static {
		System.setProperty("mockStuff", "mock:stuff");
	}

	public interface Sender {
		void send();

		@Publish("${mockStuff}")
		void stuff(String arg, Integer stuff);

		void send(@Body(converter = "jaxb") Root root);
	}

	@Autowired
	Publisher publish;

	@Publish("mock:field")
	Sender sender;

	Sender other;

	@Publish("mock:method")
	public void setOther(Sender sender) {
		this.other = sender;
	}

	@Before
	public void before() {
		reset(publish);
	}

	@Test
	public void sender_send_withMarshalled() {
		ArgumentCaptor<URI> auri = uri();
		ArgumentCaptor<Method> amethod = method();
		ArgumentCaptor<Object[]> aarr = arr();

		sender.send(new Root("world"));
		verify(publish, times(1)).publish(auri.capture(), aarr.capture(),
				amethod.capture());
		assertEquals("mock:field", auri.getValue().toASCIIString());
		Object o = aarr.getValue()[0];
		assertTrue(o instanceof Root);
		assertEquals("world", ((Root) o).hello);
		assertEquals("send", amethod.getValue().getName());
	}

	@Test
	public void sender_send_withField() {
		ArgumentCaptor<URI> auri = uri();
		ArgumentCaptor<Method> amethod = method();
		ArgumentCaptor<Object[]> aarr = arr();

		sender.send();
		verify(publish, times(1)).publish(auri.capture(), aarr.capture(),
				amethod.capture());
		assertEquals("mock:field", auri.getValue().toASCIIString());
		assertNull(aarr.getValue());
		assertEquals("send", amethod.getValue().getName());
	}

	@Test
	public void sender_stuff_withStuff() {
		assertNotNull(sender);
		ArgumentCaptor<URI> auri = uri();
		ArgumentCaptor<Method> amethod = method();
		ArgumentCaptor<Object[]> aarr = arr();

		sender.stuff("abc", 2);
		verify(publish, times(1)).publish(auri.capture(), aarr.capture(),
				amethod.capture());
		assertEquals("mock:stuff", auri.getValue().toASCIIString());
		assertEquals("abc", aarr.getValue()[0]);
		assertEquals(2, aarr.getValue()[1]);
		assertEquals("stuff", amethod.getValue().getName());
	}

	@Test
	public void other_withSetter() {
		assertNotNull(other);

	}

	static ArgumentCaptor<URI> uri() {
		return ArgumentCaptor.forClass(URI.class);
	}

	static ArgumentCaptor<String> str() {
		return ArgumentCaptor.forClass(String.class);
	}

	static ArgumentCaptor<Method> method() {
		return ArgumentCaptor.forClass(Method.class);
	}

	static ArgumentCaptor<Object[]> arr() {
		return ArgumentCaptor.forClass(Object[].class);
	}

	@XmlRootElement
	public static class Root {
		public Root() {
		}

		public Root(String word) {
			this.hello = word;
		}

		@XmlAttribute
		String hello;
	}
}

@Configuration
class PublishBeanPostProcessorContext {
	@Bean
	PublishBeanPostProcessor sbpp() {
		return new PublishBeanPostProcessor();
	}

	@Bean
	SubTest subTest() {
		return new SubTest();
	}

	@Bean
	Publisher mockPublisher() {
		return Mockito.mock(Publisher.class);
	}

	@Bean
	static PropertyPlaceholderConfigurer props() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setSearchSystemEnvironment(true);
		return ppc;
	}
}
