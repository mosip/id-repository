package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.util.SupplierWithException;

/**
 * @author Manoj SP
 *
 */
public class SupplierWithExceptionTest {

	@Test
	public void testSupplierWithExceptionValidInput() {
		assertEquals("TEXT", SupplierWithException.execute(() -> toUpperCase("text")));
	}

	@Test
	public void testSupplierWithExceptionInvalidInput() {
		try {
			SupplierWithException.execute(() -> toUpperCase(null));
		} catch (IdRepoAppUncheckedException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	private String toUpperCase(String output) throws IdRepoAppException {
		return output.toUpperCase();
	}
}
