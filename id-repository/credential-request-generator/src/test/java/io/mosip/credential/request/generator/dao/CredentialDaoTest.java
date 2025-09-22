package io.mosip.credential.request.generator.dao;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore    // TODO ignored temporarily because it is causing build failure on GitHub.
public class CredentialDaoTest {

    @Mock
    private CredentialRepositary credentialRepo;

    @InjectMocks
    private CredentialDao credentialDao;

    @Mock
    private EncryptedCredentialDao encryptedCredentialDao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdate() {
        String batchId = "batch123";
        List<CredentialEntity> credentialEntities = new ArrayList<>();
        credentialDao.update(batchId, credentialEntities);
        Mockito.verify(credentialRepo).saveAll(credentialEntities);
    }

    @Test
    public void testFindByStatusCode() {
        String statusCode = "STATUS";
        int pageSize = 100;
        Pageable pageable = PageRequest.of(0, pageSize);
        Page<CredentialEntity> expectedPage = mock(Page.class);
        when(credentialRepo.findByStatusCode(statusCode, pageable)).thenReturn(expectedPage);
        Page<CredentialEntity> result = credentialDao.findByStatusCode(statusCode, pageable);
        assertEquals(expectedPage, result);
    }

    @Test
    public void testFindByStatusCodeWithEffectiveDtimes() {
        String statusCode = "STATUS";
        LocalDateTime effectiveDTimes = LocalDateTime.now();
        int pageSize = 100;
        Pageable pageable = PageRequest.of(0, pageSize);
        Page<CredentialEntity> expectedPage = mock(Page.class);
        when(credentialRepo.findByStatusCodeWithEffectiveDtimes(statusCode, effectiveDTimes, pageable)).thenReturn(expectedPage);
        Page<CredentialEntity> result = credentialDao.findByStatusCodeWithEffectiveDtimes(statusCode, effectiveDTimes, pageable);
        assertEquals(expectedPage, result);
    }

    @Test
    public void testSave() {
        CredentialEntity credential = new CredentialEntity();
        credentialDao.save(credential);
        Mockito.verify(credentialRepo).save(credential);
    }

    @Test
    public void testFindById() {
        String requestId = "request123";
        Optional<CredentialEntity> expectedCredential = Optional.of(mock(CredentialEntity.class));
        when(credentialRepo.findById(requestId)).thenReturn(expectedCredential);
        Optional<CredentialEntity> result = credentialDao.findById(requestId);
        assertEquals(expectedCredential, result);
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
        Page<CredentialEntity> page = new PageImpl<>(credentialList);
        Mockito.when(credentialRepo.findCredentialByStatusCodes(Mockito.any(),Mockito.any())).thenReturn(page);
        credentialDao.getCredentialsForReprocess("1234");
    }
}
