package com.speajus.llama.core;

import java.lang.reflect.Method;
import java.net.URI;

public interface Subscriber {

	void register(URI uri, Method method, Object bean, String beanName);

}
