package com.speajus.llama.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.converter.Converter;

import com.speajus.llama.core.LlamaException;
import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.file.ExtensionTriggerStrategy;
import com.speajus.llama.file.FilePublisher;
import com.speajus.llama.file.FileSubscriber;
import com.speajus.llama.file.TriggerStrategy;

@Configuration
public class FileContext {

	@Bean
	FilePublisher filePublisher() {
		return new FilePublisher();
	}

	@Bean
	FileSubscriber fileSubscriber() {
		return new FileSubscriber();
	}

	@LlamaQualifier
	@Bean(destroyMethod = "shutdownNow")
	ScheduledExecutorService scheduledExecutorService() {
		return Executors.newScheduledThreadPool(2);
	}

	@Bean
	@Scope("prototype")
	TriggerStrategy extensionTriggerStrategy(final String extension) {
		return new ExtensionTriggerStrategy(extension);
	}

	@Bean
	Converter<Path, File> pathFile() {
		return new Converter<Path, File>() {

			@Override
			public File convert(Path source) {
				return source.toFile();
			}
		};
	}

	@Bean
	Converter<Path, InputStream> pathInputStream() {
		return new Converter<Path, InputStream>() {

			@Override
			public InputStream convert(Path path) {
				try {
					return Files.newInputStream(path, StandardOpenOption.READ);
				} catch (IOException e) {
					throw new LlamaException(e);
				}
			}
		};
	}

}
