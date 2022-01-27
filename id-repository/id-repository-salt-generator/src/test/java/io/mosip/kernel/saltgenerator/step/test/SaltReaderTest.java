package io.mosip.kernel.saltgenerator.step.test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant;
import io.mosip.idrepository.saltgenerator.entity.IdRepoSaltEntitiesComposite;
import io.mosip.idrepository.saltgenerator.step.SaltReader;

public class SaltReaderTest {

	SaltReader saltReader = new SaltReader();

	MockEnvironment mockEnv = new MockEnvironment();
	
	private void init(int start, int end) {
		mockEnv.setProperty(SaltGeneratorConstant.START_SEQ.getValue(), String.valueOf(start));
		mockEnv.setProperty(SaltGeneratorConstant.END_SEQ.getValue(), String.valueOf(end));
		ReflectionTestUtils.setField(saltReader, "env", mockEnv);
		saltReader.initialize();
	}

	@Test
	public void testRead() {
		init(0, 1);
		IdRepoSaltEntitiesComposite saltEntity = saltReader.read();
		assertTrue(saltEntity.getIdentityEncryptSaltEntity().getId().equals(0l));
	}
	
	@Test
	public void testReadNull() {
		init(1, 0);
		assertNull(saltReader.read());
	}
}
