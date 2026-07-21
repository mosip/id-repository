package io.mosip.idrepository.saltgenerator.constant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link SaltGeneratorConstant} property keys.
 */
public class SaltGeneratorConstantTest {

	@Test
	public void startSeqKeyMatchesConfigContract() {
		assertEquals("mosip.kernel.salt-generator.start-sequence", SaltGeneratorConstant.START_SEQ.getValue());
	}

	@Test
	public void endSeqKeyMatchesConfigContract() {
		assertEquals("mosip.kernel.salt-generator.end-sequence", SaltGeneratorConstant.END_SEQ.getValue());
	}

	@Test
	public void chunkSizeKeyMatchesConfigContract() {
		assertEquals("mosip.kernel.salt-generator.chunk-size", SaltGeneratorConstant.CHUNK_SIZE.getValue());
	}

	@Test
	public void allEnumConstantsArePresent() {
		SaltGeneratorConstant[] values = SaltGeneratorConstant.values();
		assertEquals(3, values.length);
		assertNotNull(SaltGeneratorConstant.valueOf("START_SEQ"));
		assertNotNull(SaltGeneratorConstant.valueOf("END_SEQ"));
		assertNotNull(SaltGeneratorConstant.valueOf("CHUNK_SIZE"));
	}
}
