package com.speajus.llama.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;

import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.core.ParameterResolver;
import com.speajus.llama.core.PublishBeanPostProcessor;
import com.speajus.llama.core.SubscribeBeanPostProcessor;

@Configuration
@ComponentScan("com.speajus.llama.service")
@Import({ SubscribeBeanPostProcessor.class, PublishBeanPostProcessor.class })
public class LlamaContext {

	@Autowired(required = false)
	GenericConversionService conversionService;

	@Bean
	@LlamaQualifier
	public static PropertyPlaceholderConfigurer props() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setSearchSystemEnvironment(true);
		return ppc;
	}

	@Bean
	@LlamaQualifier
	ConversionService llamaConversionService(List<Converter<?, ?>> converters) {
		GenericConversionService dcs = conversionService == null ? new DefaultConversionService()
				: conversionService;
		for (Converter<?, ?> converter : converters) {
			dcs.addConverter(converter);
		}
		return dcs;
	}

	@Bean
	@LlamaQualifier
	@Scope("prototype")
	<T> Unmarshaller jaxb(Class<T> type) throws JAXBException {
		return JAXBContext.newInstance(type).createUnmarshaller();
	}

	@Bean
	Converter<String, InputStream> inputConverter() {
		return new Converter<String, InputStream>() {

			@Override
			public InputStream convert(String source) {
				return new ByteArrayInputStream(source.getBytes());
			}
		};
	}

	@Bean
	@Autowired
	ParameterResolver parameterResolver() {
		return new ParameterResolver();
	}
}
