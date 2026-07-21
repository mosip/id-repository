package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.util.SaltUtil;
import io.mosip.kernel.core.util.HMACUtils2;

/**
 * The Class SaltUtilTest.
 * 
 * @author Loganathan S
 */
public class SaltUtilTest {
	
	/**
	 * Test modulus positive divisor.
	 */
	@Test
	public void testModulus_positiveDivisor() {
		assertEquals(491, SaltUtil.getIdvidModulo("9706932491", 3));
	}
	
	/**
	 * Test modulus lesser length.
	 */
	@Test
	public void testModulus_lesserLength() {
		assertEquals(91, SaltUtil.getIdvidModulo("91", 3));
	}
	
	/**
	 * Test modulus negative divisor.
	 */
	@Test(expected = Exception.class)
	public void testModulus_negativeDivisor() {
		SaltUtil.getIdvidModulo("9706932491", -3);
	}
	
	/**
	 * Test modulus 0 divisor.
	 */
	@Test(expected = Exception.class)
	public void testModulus_0Divisor() {
		SaltUtil.getIdvidHashModulo("9706932491", 0);
	}
	
	/**
	 * Test modulus positive divisor.
	 */
	@Test
	public void testHashModulus_positiveDivisor() {
		assertEquals(413, SaltUtil.getIdvidHashModulo("9706932491", 3)); 
	}
	
	/**
	 * Test modulus lesser length.
	 */
	@Test
	public void testHashModulus_lesserLength() {
		assertEquals(568, SaltUtil.getIdvidHashModulo("91", 3));
	}
	
	/**
	 * Test modulus negative divisor.
	 */
	@Test(expected = Exception.class)
	public void testHashModulus_negativeDivisor() {
		SaltUtil.getIdvidHashModulo("9706932491", -3);
	}
	
	/**
	 * Test modulus 0 divisor.
	 */
	@Test(expected = Exception.class)
	public void testHashModulus_0Divisor() {
		SaltUtil.getIdvidModulo("9706932491", 0); 
	}

	@Test
	public void testHashModulusNoSuchAlgorithmException() {
		NoSuchAlgorithmException cause = new NoSuchAlgorithmException("SHA-256");
		try (MockedStatic<HMACUtils2> hmac = Mockito.mockStatic(HMACUtils2.class)) {
			hmac.when(() -> HMACUtils2.digestAsPlainText(Mockito.any(byte[].class))).thenThrow(cause);
			try {
				SaltUtil.getIdvidHashModulo("9706932491", 3);
				org.junit.Assert.fail("Expected IdRepoAppUncheckedException");
			} catch (IdRepoAppUncheckedException ex) {
				assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), ex.getErrorCode());
				assertSame(cause, ex.getCause());
			}
		}
	}

}
