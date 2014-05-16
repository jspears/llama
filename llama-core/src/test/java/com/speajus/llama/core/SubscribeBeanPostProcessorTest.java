package com.speajus.llama.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.net.URI;

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

import com.speajus.llama.annotation.Subscribe;
import com.speajus.llama.core.SubscribeBeanPostProcessorTest.SubTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SubscribeContext.class)
public class SubscribeBeanPostProcessorTest {
	static {
		System.setProperty("mockSub", "mock:sub");
	}
	@Autowired
	Subscriber mock;

	@Autowired
	SubTest test;

	@Test
	public void test() {
		ArgumentCaptor<URI> uric = ArgumentCaptor.forClass(URI.class);
		ArgumentCaptor<Method> methodc = ArgumentCaptor.forClass(Method.class);
		ArgumentCaptor<Object> objectc = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> stringc = ArgumentCaptor.forClass(String.class);
		verify(mock, times(1)).register(uric.capture(), methodc.capture(),
				objectc.capture(), stringc.capture());
		assertEquals("subTest", stringc.getValue());
		assertEquals("method", methodc.getValue().getName());
	}

	public static class SubTest {
		@Subscribe("${mockSub}")
		public void method(String arg) {

		}
	}
}

@Configuration
class SubscribeContext {
	@Bean
	SubscribeBeanPostProcessor sbpp() {
		return new SubscribeBeanPostProcessor();
	}

	@Bean
	SubTest subTest() {
		return new SubTest();
	}

	@Bean
	Subscriber mockSubscriber() {
		return Mockito.mock(Subscriber.class);
	}

	@Bean
	PropertyPlaceholderConfigurer props() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setSearchSystemEnvironment(true);
		return ppc;
	}
}
