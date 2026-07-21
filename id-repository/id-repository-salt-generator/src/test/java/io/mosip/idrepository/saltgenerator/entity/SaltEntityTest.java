package io.mosip.idrepository.saltgenerator.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;

import org.junit.Test;

import io.mosip.idrepository.saltgenerator.entity.idmap.VidEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidHashSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;

/**
 * Unit tests for salt entity getters/setters via {@link ISaltEntity}.
 */
public class SaltEntityTest {

	@Test
	public void identityHashSaltEntityRoundTripsFields() {
		assertEntityRoundTrip(new IdentityHashSaltEntity());
	}

	@Test
	public void identityEncryptSaltEntityRoundTripsFields() {
		assertEntityRoundTrip(new IdentityEncryptSaltEntity());
	}

	@Test
	public void vidHashSaltEntityRoundTripsFields() {
		assertEntityRoundTrip(new VidHashSaltEntity());
	}

	@Test
	public void vidEncryptSaltEntityRoundTripsFields() {
		assertEntityRoundTrip(new VidEncryptSaltEntity());
	}

	private static void assertEntityRoundTrip(ISaltEntity entity) {
		LocalDateTime created = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime updated = LocalDateTime.of(2026, 1, 2, 0, 0);

		entity.setId(7L);
		entity.setSalt("salt-value");
		entity.setCreatedBy("System");
		entity.setCreateDtimes(created);
		entity.setUpdatedBy("admin");
		entity.setUpdatedDtimes(updated);

		assertEquals(Long.valueOf(7L), entity.getId());
		assertEquals("salt-value", entity.getSalt());
		assertEquals("System", entity.getCreatedBy());
		assertEquals(created, entity.getCreateDtimes());
		assertEquals("admin", entity.getUpdatedBy());
		assertEquals(updated, entity.getUpdatedDtimes());

		entity.setUpdatedBy(null);
		entity.setUpdatedDtimes(null);
		assertNull(entity.getUpdatedBy());
		assertNull(entity.getUpdatedDtimes());
	}
}
