package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.mosip.idrepository.core.util.IdRepositoryUtil;

public class IdRepositoryUtilTest {

	@Test
	public void testTrimAndLowercase() {
		assertEquals("abc@phone", IdRepositoryUtil.getTrimmedLowercaseValue("  ABC@Phone  "));
	}

	@Test
	public void testNull() {
		assertNull(IdRepositoryUtil.getTrimmedLowercaseValue(null));
	}
}
