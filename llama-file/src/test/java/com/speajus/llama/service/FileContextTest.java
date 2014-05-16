package com.speajus.llama.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.speajus.llama.annotation.Header;
import com.speajus.llama.annotation.Subscribe;
import com.speajus.llama.core.util.FileUtil;
import com.speajus.llama.service.FileContextTest.SubscribeTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FileContextTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		FileUtil.removeRecursive(new File("target/test").toPath());
	}

	@Autowired
	SubscribeTest onFile;

	@Test
	public void writeFile_Await() throws Exception {
		touch("file", UUID.randomUUID().toString());
		assertTrue(onFile.file.await(4, TimeUnit.MINUTES));
	}

	@Test
	public void writeFile_AndTriggerAwait() throws Exception {
		final String test = "triggerme";
		touch("trigger", test);
		Thread.sleep(2000);
		assertEquals(1L, onFile.trigger.getCount());
		touch("trigger", test + ".go");

		assertTrue(onFile.trigger.await(1, TimeUnit.MINUTES));
	}

	void touch(String dir, String file) throws FileNotFoundException,
			IOException {
		File fdir = new File("target/test/" + dir);
		fdir.mkdirs();
		File out = new File(fdir, file);
		new FileOutputStream(out).close();
		System.out.println("wrote " + out.getAbsolutePath());
	}

	public static class SubscribeTest {
		CountDownLatch file, trigger;

		public SubscribeTest(int count) {
			file = new CountDownLatch(count);
			trigger = new CountDownLatch(count);
		}

		@Subscribe("file://./target/test/file")
		public void onFile(File f, @Header Map<String, Object> map) {
			file.countDown();
		}

		@Subscribe("file://./target/test/trigger?triggerExtension=.go")
		public void onTrigger(File file, @Header Map<String, Object> map) {
			trigger.countDown();
		}
	}

}

@Import(LlamaContext.class)
@Configuration
class TestContext {
	@Bean
	SubscribeTest testBean() {
		return new SubscribeTest(1);
	}
}