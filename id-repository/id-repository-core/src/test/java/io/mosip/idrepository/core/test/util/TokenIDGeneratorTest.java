package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.util.HMACUtils2;

public class TokenIDGeneratorTest {

	private TokenIDGenerator generator;

	@Before
	public void setup() throws Exception {
		generator = new TokenIDGenerator();
		setField(generator, "uinSalt", "UIN_SALT");
		setField(generator, "partnerCodeSalt", "PARTNER_SALT");
		setField(generator, "tokenIDLength", 10);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field f = TokenIDGenerator.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	public void testGenerateTokenIDSuccess() throws Exception {
		String uin = "123456";
		String partner = "PART1";

		String uinHash = HMACUtils2.digestAsPlainText((uin + "UIN_SALT").getBytes());
		String finalHash = HMACUtils2.digestAsPlainText(("PARTNER_SALT" + partner + uinHash).getBytes());
		String expectedToken = new java.math.BigInteger(finalHash.getBytes()).toString().substring(0, 10);

		assertEquals(expectedToken, generator.generateTokenID(uin, partner));
	}
}
