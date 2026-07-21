package io.mosip.idrepository.core.test.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

import io.mosip.idrepository.core.bootstrap.ContextClassLoaderHolder;
import io.mosip.idrepository.core.bootstrap.ContextClassLoaderRunnable;

public class ContextClassLoaderRunnableTest {

	@After
	public void tearDown() {
		ContextClassLoaderHolder.set(null);
	}

	@Test
	public void wrapRunsTaskWithApplicationClassLoader() {
		ClassLoader loader = new ClassLoader() {
		};
		ContextClassLoaderHolder.set(loader);
		AtomicReference<ClassLoader> observed = new AtomicReference<>();
		ContextClassLoaderRunnable.wrap(() -> observed.set(Thread.currentThread().getContextClassLoader())).run();
		assertEquals(loader, observed.get());
	}

	@Test
	public void threadFactorySetsContextClassLoaderOnCreatedThread() throws Exception {
		ClassLoader loader = new ClassLoader() {
		};
		ContextClassLoaderHolder.set(loader);
		AtomicReference<ClassLoader> observed = new AtomicReference<>();
		Thread thread = ContextClassLoaderRunnable.threadFactory().newThread(() -> {
			observed.set(Thread.currentThread().getContextClassLoader());
		});
		thread.start();
		thread.join();
		assertEquals(loader, observed.get());
		assertTrue(thread.getName().startsWith("idrepo-async-"));
	}

	@Test
	public void delegatingThreadFactoryWrapsDelegate() throws Exception {
		ClassLoader loader = new ClassLoader() {
		};
		ContextClassLoaderHolder.set(loader);
		AtomicBoolean ran = new AtomicBoolean(false);
		Thread thread = ContextClassLoaderRunnable.delegatingThreadFactory(r -> new Thread(r, "custom")).newThread(() -> {
			ran.set(true);
			assertEquals(loader, Thread.currentThread().getContextClassLoader());
		});
		thread.start();
		thread.join();
		assertTrue(ran.get());
		assertEquals("custom", thread.getName());
	}
}
