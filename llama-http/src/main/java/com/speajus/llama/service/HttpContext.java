package com.speajus.llama.service;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.client.RestTemplate;

import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.http.HttpPublisher;
@Configuration
public class HttpContext {

	@Autowired(required = false)
	CloseableHttpClient httpClient;

	@Bean
	ExpressionParser parser() {
		return new SpelExpressionParser();
	}

	@Bean
	@Autowired
	BeanFactoryResolver beanFactoryResolver(ApplicationContext ctx) {
		return new BeanFactoryResolver(ctx);
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@LlamaQualifier
	@Bean
	CloseableHttpClient httpClient() {
		if (httpClient != null) {
			return httpClient;
		}
		return (httpClient = HttpClients.createDefault());
	};

	@Bean
	HttpPublisher httpPublisher() {
		return new HttpPublisher();
	}
}
