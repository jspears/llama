package com.speajus.llama.service;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(LlamaContext.class)
public class TomcatContext {
	@Value("${llama.tomcat.http.port:8080}")
	int httpPort;

	@Bean
	ServletContext createTomcatContext() throws LifecycleException {
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(httpPort);
		Context ctx = tomcat.addContext("/",
				new File("src/main/webapp").getAbsolutePath());
		tomcat.start();
//		tomcat.getServer().await();
		return ctx.getServletContext();
	}
}
