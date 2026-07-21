package io.mosip.idrepository.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;

public class TokenIDGeneratorExceptionTest {

	private FailingTokenIDGenerator generator;

	@Before
	public void setup() throws Exception {
		generator = new FailingTokenIDGenerator();
		setField(generator, "uinSalt", "UIN_SALT");
		setField(generator, "partnerCodeSalt", "PARTNER_SALT");
		setField(generator, "tokenIDLength", 10);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = TokenIDGenerator.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	public void testGenerateTokenIDNoSuchAlgorithmException() {
		NoSuchAlgorithmException cause = new NoSuchAlgorithmException("SHA-256");
		generator.failWith(cause);

		try {
			generator.generateTokenID("123456", "PART1");
			org.junit.Assert.fail("Expected IdRepoAppUncheckedException");
		} catch (IdRepoAppUncheckedException ex) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), ex.getErrorCode());
			assertSame(cause, ex.getCause());
		}
	}

	private static final class FailingTokenIDGenerator extends TokenIDGenerator {
		private NoSuchAlgorithmException failure;

		void failWith(NoSuchAlgorithmException failure) {
			this.failure = failure;
		}

		@Override
		String digestAsPlainText(byte[] data) throws NoSuchAlgorithmException {
			if (failure != null) {
				throw failure;
			}
			return super.digestAsPlainText(data);
		}
	}
}
