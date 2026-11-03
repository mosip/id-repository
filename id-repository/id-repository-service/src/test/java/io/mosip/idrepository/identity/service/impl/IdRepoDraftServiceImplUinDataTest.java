package io.mosip.idrepository.identity.service.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.entity.UinDraft;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.repository.UinRepo;

/**
 * Tests {@code updateDraftUinData} for a LOST draft that has no identity data yet.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class IdRepoDraftServiceImplUinDataTest {

	@InjectMocks
	private IdRepoDraftServiceImpl service;

	@Mock
	private UinDraftRepo uinDraftRepo;

	@Mock
	private UinRepo uinRepo;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Mock
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Before
	public void setUp() throws IdRepoAppException {
		ReflectionTestUtils.setField(service, "mapper", new ObjectMapper());
		ReflectionTestUtils.setField(service, "uinRefId", "uin-ref");
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
		when(securityManager.hash(any())).thenReturn("data-hash");
	}

	// #1617: stamping a LOST draft before any updateDraftV2 call used to fail with
	// IDR-IDC-004 (NullPointerException on the draft's null uinData).
	@Test
	public void copiesLiveIdentity_whenDraftHasNoUinData() throws IdRepoAppException, IOException {
		UinDraft draft = new UinDraft();
		draft.setRegId("REG123");
		Uin liveUin = new Uin();
		liveUin.setUinData("{\"UIN\":\"274390482564\",\"email\":\"live@mosip.net\"}".getBytes());
		when(uinDraftRepo.findByRegId("REG123")).thenReturn(Optional.of(draft));
		when(uinRepo.findByUinHash(any())).thenReturn(Optional.of(liveUin));
		when(uinRepo.existsByUinHash(any())).thenReturn(true);

		IdResponseDTO response = service.updateDraftUinData("REG123", "274390482564");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertEquals("1234_some-hash", draft.getUinHash());
		assertArrayEquals(liveUin.getUinData(), draft.getUinData());
		assertEquals("data-hash", draft.getUinDataHash());
		verify(uinDraftRepo).flush();
	}
}
