package com.speajus.llama.core.util;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;

public class TypeUtilTest {

	@Test
	public void testCanAssign() {
		assertTrue(TypeUtil.canAssign("test", String.class));
		assertTrue(TypeUtil.canAssign("test", CharSequence.class));
		assertTrue(TypeUtil.canAssign(null, CharSequence.class));

	}

	@Test
	public void testCanAssignNullNotOK() {
		assertFalse(TypeUtil.canAssignNullNotOK(null, CharSequence.class));
	}
	@Test
	public void isAssignable() throws Exception {
		assertTrue(InputStream.class.isAssignableFrom(FileInputStream.class));
		assertFalse(FileInputStream.class.isAssignableFrom(InputStream.class));
	}
}
