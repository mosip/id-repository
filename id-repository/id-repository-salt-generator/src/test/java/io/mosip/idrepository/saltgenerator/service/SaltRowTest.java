package io.mosip.idrepository.saltgenerator.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.LocalDateTime;

import org.junit.Test;

/**
 * Unit tests for {@link SaltRow} record accessors and equality.
 */
public class SaltRowTest {

	@Test
	public void accessorsReturnConstructorValues() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 10, 12, 0);
		SaltRow row = new SaltRow(42L, "hash", "idEnc", "vidEnc", createdAt);

		assertEquals(42L, row.id());
		assertEquals("hash", row.hashSalt());
		assertEquals("idEnc", row.identityEncryptSalt());
		assertEquals("vidEnc", row.vidEncryptSalt());
		assertEquals(createdAt, row.createdAt());
	}

	@Test
	public void equalsAndHashCodeUseAllComponents() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 10, 12, 0);
		SaltRow a = new SaltRow(1L, "h", "i", "v", createdAt);
		SaltRow b = new SaltRow(1L, "h", "i", "v", createdAt);
		SaltRow c = new SaltRow(2L, "h", "i", "v", createdAt);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}
}
