package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.mosip.idrepository.core.util.SaltUtil;

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
		SaltUtil.getIdvidModulo("9706932491", 0);
	}

}
