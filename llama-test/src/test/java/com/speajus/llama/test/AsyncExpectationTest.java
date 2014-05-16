package com.speajus.llama.test;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncExpectationTest {

	@Test
	public void test() throws InterruptedException {
		AsyncExpectation async = AsyncExpectation.expect(1);
		async.expectEquals(10, 5);
		assertFalse(async.verify(10, TimeUnit.SECONDS));
	}

}
