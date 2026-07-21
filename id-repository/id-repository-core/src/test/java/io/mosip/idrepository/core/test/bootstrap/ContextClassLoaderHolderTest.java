package io.mosip.idrepository.core.test.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Test;

import io.mosip.idrepository.core.bootstrap.ContextClassLoaderHolder;

public class ContextClassLoaderHolderTest {

	@After
	public void tearDown() {
		ContextClassLoaderHolder.set(null);
	}

	@Test
	public void getReturnsHolderWhenSet() {
		ClassLoader loader = new ClassLoader() {
		};
		ContextClassLoaderHolder.set(loader);
		assertSame(loader, ContextClassLoaderHolder.get());
	}

	@Test
	public void getFallsBackToThreadContextClassLoaderWhenUnset() {
		ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
		assertEquals(contextLoader, ContextClassLoaderHolder.get());
	}
}
