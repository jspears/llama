package com.speajus.llama.core;

import java.lang.reflect.Method;
import java.net.URI;

public interface Publisher {

	Object publish(URI uri, Object[] args, Method method);
}
