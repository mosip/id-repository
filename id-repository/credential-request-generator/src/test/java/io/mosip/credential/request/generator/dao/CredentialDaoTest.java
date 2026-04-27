package io.mosip.credential.request.generator.dao;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;

public class CredentialDaoTest {

	private CredentialRepositary<CredentialEntity, String> credentialRepo;

	@InjectMocks
	private CredentialDao credentialDao;
	
	@Mock
	private EncryptedCredentialDao encryptedCredentialDao;

	private CredentialRepositoryStub credentialRepositoryStub;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		credentialRepositoryStub = new CredentialRepositoryStub();
		credentialRepo = credentialRepositoryStub.createProxy();
		ReflectionTestUtils.setField(credentialDao, "status",
				"NEW");
		ReflectionTestUtils.setField(credentialDao, "pageSize",
				1);
		ReflectionTestUtils.setField(credentialDao, "reprocessStatusCodes",
				"FAILED");
		ReflectionTestUtils.setField(credentialDao, "credentialRepo",
				credentialRepo);
		ReflectionTestUtils.setField(credentialDao, "encryptedCredentialDao",
				encryptedCredentialDao);
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
		List<CredentialEntity> credentialList = new ArrayList<>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Mockito.when(encryptedCredentialDao.getCredentialByStatus(Mockito.anyString(), Mockito.anyInt()))
				.thenReturn(credentialList);
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
		credentialRepositoryStub.credentialsByStatusCodes = credentialList;
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
		Pageable pageable=PageRequest.of(0, 1);
		credentialRepositoryStub.statusCodeResult = credentialList;
		credentialDao.findByStatusCode("NEW", pageable);
	}
	
	@Test
	public void testfindByStatusCodeWithEffectiveDtimes(){
		List<CredentialEntity> credentialList=new ArrayList<CredentialEntity>();
		CredentialEntity credentialEntity = new CredentialEntity();
		credentialEntity.setRequestId("1234");
		credentialEntity.setRequest("test");
		credentialEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credentialList.add(credentialEntity);
		Pageable pageable=PageRequest.of(0, 1);
		credentialRepositoryStub.statusCodeWithEffectiveDtimesResult = credentialList;
		credentialDao.findByStatusCodeWithEffectiveDtimes("NEW", LocalDateTime.now(ZoneId.of("UTC")),pageable);
	}

	private static class CredentialRepositoryStub implements InvocationHandler {

		private List<CredentialEntity> statusCodeResult = new ArrayList<>();
		private List<CredentialEntity> statusCodeWithEffectiveDtimesResult = new ArrayList<>();
		private List<CredentialEntity> credentialsByStatusCodes = new ArrayList<>();

		@SuppressWarnings("unchecked")
		private CredentialRepositary<CredentialEntity, String> createProxy() {
			return (CredentialRepositary<CredentialEntity, String>) Proxy.newProxyInstance(
					CredentialRepositary.class.getClassLoader(),
					new Class<?>[] { CredentialRepositary.class },
					this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			String methodName = method.getName();
			if ("save".equals(methodName)) {
				return args[0];
			}
			if ("saveAll".equals(methodName)) {
				return args[0];
			}
			if ("findByStatusCode".equals(methodName)) {
				return new PageImpl<>(statusCodeResult);
			}
			if ("findByStatusCodeWithEffectiveDtimes".equals(methodName)) {
				return new PageImpl<>(statusCodeWithEffectiveDtimesResult);
			}
			if ("findCredentialByStatusCodes".equals(methodName)) {
				return credentialsByStatusCodes;
			}
			if ("toString".equals(methodName)) {
				return "CredentialRepositoryStub";
			}
			return null;
		}
	}
}
