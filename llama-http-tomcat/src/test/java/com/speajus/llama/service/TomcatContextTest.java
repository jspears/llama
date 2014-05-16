package com.speajus.llama.service;

import static org.junit.Assert.*;

import javax.servlet.ServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.speajus.llama.annotation.Subscribe;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = LlamaContext.class)
public class TomcatContextTest {

	@Autowired
	HttpContextTest test;

	@Test
	public void test() {
		assertNotNull(test);
	}

	@Component
	class HttpContextTest {
		@Subscribe("http://localhost/something")
		public void onHttpRequest(ServletRequest request) {
			System.out.println("heere I am");
		}
	}
}