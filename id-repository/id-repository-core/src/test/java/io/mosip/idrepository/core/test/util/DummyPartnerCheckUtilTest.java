package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;

/**
 * @author Manoj SP
 *
 */
public class DummyPartnerCheckUtilTest {

	DummyPartnerCheckUtil util = new DummyPartnerCheckUtil();

	@Before
	public void init() {
		ReflectionTestUtils.setField(util, "dummyOLVPartnerId", "partner");
	}

	@Test
	public void testGetDummyPartnerId() {
		assertEquals("partner", util.getDummyOLVPartnerId());
	}

	@Test
	public void testIsDummyOLVPartner() {
		assertTrue(util.isDummyOLVPartner("partner"));
		assertFalse(util.isDummyOLVPartner(""));
	}
}
