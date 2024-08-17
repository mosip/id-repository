package io.mosip.credential.request.generator.dao;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import io.mosip.idrepository.core.util.EnvUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
@Ignore
public class CredentialDaoTest {

	@Mock
	private CredentialRepositary<CredentialEntity, String> credentialRepo;

	@InjectMocks
	private CredentialDao credentialDao;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(credentialDao, "status",
				"NEW");
		ReflectionTestUtils.setField(credentialDao, "pageSize",
				1);
		ReflectionTestUtils.setField(credentialDao, "reprocessStatusCodes",
				"FAILED");
		ReflectionTestUtils.setField(credentialDao, "credentialRepo",
				credentialRepo);
	}
	
	@Test
	public void testUpdateEntities(){
		List<CredentialEntity> credentialEntities=new ArrayList<>();
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		credentialEntities.add(credentialEntity);
		credentialDao.update("1234",credentialEntities);
		
	}

	@Test
	public void testUpdate() {
		String batchId = "batch123";
		List<CredentialEntity> credentialEntities = new ArrayList<>();
		credentialDao.update(batchId, credentialEntities);
		//Mockito.verify(crdentialRepo).saveAll(credentialEntities);
	}

	@Test
	public void testSave(){
		CredentialEntity credentialEntity=new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setStatusCode("NEW");
		credentialDao.save(credentialEntity);
		
	}
	@Test
	public void testGetCredentials(){
		List<CredentialEntity> credentialList=new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);
		Mockito.when(credentialRepo.findCredentialByStatusCode(Mockito.any(), Mockito.any())).thenReturn(page);
		credentialDao.getCredentials("1234");
	}
	
	@Test
	public void testGetCredentialsForReprocess(){
		List<CredentialEntity> credentialList=new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);
		Mockito.when(credentialRepo.findCredentialByStatusCodes(Mockito.any(),Mockito.any())).thenReturn(page);
		credentialDao.getCredentialsForReprocess("1234");
	}
	@Test
	public void testFindByStatusCode(){
		List<CredentialEntity> credentialList=new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);
		Pageable pageable=PageRequest.of(0, 1);
		Mockito.when(credentialRepo.findByStatusCode(Mockito.any(), Mockito.any())).thenReturn(page);
		credentialDao.findByStatusCode("NEW", pageable);
	}
	
	public void testfindByStatusCodeWithEffectiveDtimes(){
		List<CredentialEntity> credentialList=new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Page<CredentialEntity> page = new PageImpl<>(credentialList);
		Pageable pageable=PageRequest.of(0, 1);
		Mockito.when(credentialRepo.findByStatusCodeWithEffectiveDtimes(Mockito.any(), Mockito.any(),  Mockito.any())).thenReturn(page);
		credentialDao.findByStatusCodeWithEffectiveDtimes("NEW", LocalDateTime.now(ZoneId.of("UTC")),pageable);
	}
}