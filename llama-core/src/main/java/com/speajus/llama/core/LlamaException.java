package com.speajus.llama.core;

public class LlamaException extends RuntimeException {

	public LlamaException() {
		super();
	}

	public LlamaException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public LlamaException(String message, Throwable cause) {
		super(message, cause);
	}

	public LlamaException(String message) {
		super(message);
	}

	public LlamaException(Throwable cause) {
		super(cause);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 738648821123273203L;

}
