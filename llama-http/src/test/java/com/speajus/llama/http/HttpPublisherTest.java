package com.speajus.llama.http;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import com.speajus.llama.annotation.Header;
import com.speajus.llama.service.LlamaContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = LlamaContext.class)
public class HttpPublisherTest {

	@Autowired
	HttpPublisher publish;

	public static class Stuff {
		@Header(key = "hi", value = "hi")
		String send(String me) {
			return "yes";
		}
	}

	@Test
	public void test() throws NoSuchMethodException, SecurityException {
		final Method m = ReflectionUtils.findMethod(Stuff.class, "send",
				String.class);
		String out = publish.resolve("#headers['hi']", m, "me");
		assertEquals("hi", out);
	}

}
