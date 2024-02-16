package io.mosip.credential.request.generator.dao;

import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;

@RunWith(SpringRunner.class)
public class CredentialDaoTest {

	@InjectMocks
	private CredentialDao credentialDao;

	@Mock
	private CredentialRepositary<CredentialEntity, String> credentialRepo;

	@Test
	public void testUpdate() {
		when(credentialRepo.saveAll(List.of())).thenReturn(List.of());
		credentialDao.update("1234567890", List.of());
	}

	@Test
	public void testFindByStatusCode() {
		Pageable pageable = PageRequest.of(0, 100);
		Page<CredentialEntity> page = new PageImpl<>(List.of(new CredentialEntity()));
		when(credentialRepo.findByStatusCode("status", pageable)).thenReturn(page);
		credentialDao.findByStatusCode("status", pageable);
	}

	@Test
	public void testFindByStatusCodeWithEffectiveDtimes() {
		Pageable pageable = PageRequest.of(0, 100);
		Page<CredentialEntity> page = new PageImpl<>(List.of(new CredentialEntity()));
		when(credentialRepo.findByStatusCodeWithEffectiveDtimes("status", LocalDateTime.now(), pageable))
				.thenReturn(page);
		credentialDao.findByStatusCodeWithEffectiveDtimes("status", LocalDateTime.now(), pageable);
	}

	@Test
	public void testSave() {
		when(credentialRepo.save(new CredentialEntity())).thenReturn(new CredentialEntity());
		credentialDao.save(new CredentialEntity());
	}

	@Test
	public void testFindById() {
		when(credentialRepo.findById("1234567890")).thenReturn(Optional.of(new CredentialEntity()));
		credentialDao.findById("1234567890");
	}

}
