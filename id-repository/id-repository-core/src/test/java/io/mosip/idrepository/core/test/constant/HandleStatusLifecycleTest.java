package io.mosip.idrepository.core.test.constant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.mosip.idrepository.core.constant.HandleStatusLifecycle;

public class HandleStatusLifecycleTest {

	@Test
	public void testGetHandleStatusKnownValues() {
		assertEquals(HandleStatusLifecycle.ACTIVATED, HandleStatusLifecycle.getHandleStatus("ACTIVATED"));
		assertEquals(HandleStatusLifecycle.DELETE, HandleStatusLifecycle.getHandleStatus("DELETE"));
		assertEquals(HandleStatusLifecycle.DELETE_REQUESTED, HandleStatusLifecycle.getHandleStatus("DELETE_REQUESTED"));
	}

	@Test
	public void testGetHandleStatusUnknown() {
		assertNull(HandleStatusLifecycle.getHandleStatus("UNKNOWN"));
		assertNull(HandleStatusLifecycle.getHandleStatus(null));
	}
}
