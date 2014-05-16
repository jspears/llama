package com.speajus.llama.http;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.MethodParameter;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.core.ParameterResolver;
import com.speajus.llama.core.Publisher;
import com.speajus.llama.core.util.HeaderUtil;

public class HttpPublisher implements Publisher {

	@LlamaQualifier
	@Autowired
	HttpClient client;

	@Autowired
	ParameterResolver resolver;

	@Autowired
	ExpressionParser parser;

	@Autowired
	BeanFactoryResolver beanFactoryResolver;

	protected String resolve(String value, Method method, Object... args) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setBeanResolver(beanFactoryResolver);
		Map<String, Object> map = HeaderUtil.headersFrom(method);
		Map<String, Object> vars = new HashMap<>();
		for (MethodParameter mp : ParameterResolver.parameters(method)) {
			final String key = mp.getParameterName();
			final String pos = "p" + mp.getParameterIndex();
			if (key != null) {
				vars.put(key, args[mp.getParameterIndex()]);
			}
			vars.put(pos, args[mp.getParameterIndex()]);
		}
		vars.put("headers", map);

		context.setVariables(vars);
		final Expression expr = parser.parseExpression(value);
		final String rtvalue = expr.getValue(context, String.class);
		return rtvalue;
	}

	@Override
	public Object publish(URI uri, Object[] args, Method method) {

		HttpGet httpGet = new HttpGet("http://targethost/homepage");
		return null;
	}

}
