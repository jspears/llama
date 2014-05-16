package com.speajus.llama.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;

public class AsyncExpectation {
	final protected CountDownLatch latch;
	final Queue<Expectation> expectations = new ConcurrentLinkedQueue<>();

	public AsyncExpectation(int count) {
		latch = new CountDownLatch(count);
	}

	public static AsyncExpectation expect(int count) {
		return new AsyncExpectation(count);
	}

	public boolean verify(long timeout, TimeUnit unit)
			throws InterruptedException {
		boolean result = latch.await(timeout, unit);
		if (result) {
			result = isSatisfied();
		}
		return result;
	}

	protected boolean isSatisfied() {
		List<Expectation> list = new ArrayList<>();
		while (!expectations.isEmpty()) {
			Expectation e = expectations.poll();
			if (e != null) {
				if (!e.satisfied()) {
					list.add(e);
				}
			}
		}
		expectations.addAll(list);
		return true;
	}

	public AsyncExpectation add(Expectation expectation) {
		expectations.add(expectation);
		return this;

	}

	public AsyncExpectation expectEquals(final Object expected,
			final Object actual) {
		add(new Expectation() {

			@Override
			public boolean satisfied() {
				assertEquals(expected, actual);

				return true;
			}
		});

		return this;
	}

	public static interface Expectation {
		public boolean satisfied();
	}

	public static class MatcherExpectation<T> implements Expectation {
		Matcher<T> matcher;
		T expected;

		@Override
		public boolean satisfied() {
			assertThat(expected, matcher);
			return true;
		}

	}
}
