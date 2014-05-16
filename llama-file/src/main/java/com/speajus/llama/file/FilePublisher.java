package com.speajus.llama.file;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;

import com.speajus.llama.core.LlamaQualifier;
import com.speajus.llama.core.Publisher;

public class FilePublisher implements Publisher {

	@Value("${llama.file.pollTime:10000}")
	protected long pollTime;
	
	@Value("${llama.file.pollTimeUnit:MILLISECONDS}")
	protected TimeUnit pollTimeUnite;
	
	@Autowired
	@LlamaQualifier
	protected ConversionService conversionService;
	
	protected final String triggerExtension;
	
	public FilePublisher() {
		this(null);
	}
	public FilePublisher(String triggerExtension) {
		this.triggerExtension = triggerExtension;
	}
	
	
	
	
	@Override
	public Object publish(URI uri, Object[] args, Method method) {
		// TODO Auto-generated method stub
		return null;
	}

}
