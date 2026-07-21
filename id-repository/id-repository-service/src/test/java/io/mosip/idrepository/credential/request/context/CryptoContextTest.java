package io.mosip.idrepository.credential.request.context;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CryptoContextTest {

	@After
	public void tearDown() {
		CryptoContext.clearSkipDecryption();
	}

	@Test
	public void defaultSkipDecryptionIsFalse() {
		assertFalse(CryptoContext.isSkipDecryption());
	}

	@Test
	public void setAndGetSkipDecryption() {
		CryptoContext.setSkipDecryption(true);
		assertTrue(CryptoContext.isSkipDecryption());

		CryptoContext.setSkipDecryption(false);
		assertFalse(CryptoContext.isSkipDecryption());
	}

	@Test
	public void defaultConstructorIsAccessibleForCoverage() throws Exception {
		Constructor<CryptoContext> constructor = CryptoContext.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void newThreadGetsDefaultSkipDecryption() throws Exception {
		java.util.concurrent.atomic.AtomicBoolean value = new java.util.concurrent.atomic.AtomicBoolean(true);
		Thread thread = new Thread(() -> value.set(CryptoContext.isSkipDecryption()));
		thread.start();
		thread.join();
		assertFalse(value.get());
	}

	@Test
	public void threadLocalCanBeUpdatedOnBackgroundThread() throws Exception {
		Thread thread = new Thread(() -> {
			CryptoContext.setSkipDecryption(true);
			assertTrue(CryptoContext.isSkipDecryption());
			CryptoContext.clearSkipDecryption();
			assertFalse(CryptoContext.isSkipDecryption());
		});
		thread.start();
		thread.join();
		assertFalse(CryptoContext.isSkipDecryption());
	}

	@Test
	public void clearSkipDecryptionResetsToDefault() {
		CryptoContext.setSkipDecryption(true);
		CryptoContext.clearSkipDecryption();
		assertFalse(CryptoContext.isSkipDecryption());
	}
}
