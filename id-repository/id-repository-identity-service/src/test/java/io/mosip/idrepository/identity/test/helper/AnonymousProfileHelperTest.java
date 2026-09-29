package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import io.mosip.idrepository.identity.helper.AnonymousProfileHelper;
import io.mosip.idrepository.identity.helper.ChannelInfoHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.repository.AnonymousProfileRepo;
import io.mosip.kernel.core.util.CryptoUtil;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class AnonymousProfileHelperTest {

	@InjectMocks
	private AnonymousProfileHelper anonymousProfileHelper;

	@Mock
	private AnonymousProfileRepo anonymousProfileRepo;

	@Autowired
	private ObjectMapper mapper;

	@Mock
	private ObjectStoreHelper objectStoreHelper;

	@Mock
	private ChannelInfoHelper channelInfoHelper;

	@Mock
	private Executor anonymousProfileExecutor;

	IdentityMapping identityMapping;

	private String cbeff;

	private String identityData;

	@Before
	public void init() throws Exception {
		ReflectionTestUtils.setField(anonymousProfileHelper, "mapper", mapper);
		ReflectionTestUtils.setField(anonymousProfileHelper, "identityMappingJson", "");
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		identityMapping = mapper.readValue(
				IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-mapping.json"),
						StandardCharsets.UTF_8),
				IdentityMapping.class);
		IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu/MM/dd");
		doAnswer(invocation -> {
			invocation.<Runnable>getArgument(0).run();
			return null;
		}).when(anonymousProfileExecutor).execute(any(Runnable.class));
	}

    @Test
    public void testBuildAndsaveProfile() throws JsonProcessingException {
        // Run the method under test
        anonymousProfileHelper
                .setRegId("1")
                .setNewUinData(identityData.getBytes())
                .setNewCbeff(cbeff)
                .setOldCbeff(cbeff)
                .setOldUinData(identityData.getBytes())
                .buildAndsaveProfile(false);

        // Prepare expected AnonymousProfileEntity
        IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
                .setFilterLanguage("eng")
                .setProcessName("Update")
                .setOldIdentity(identityData.getBytes())
                .setOldDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .setNewIdentity(identityData.getBytes())
                .setNewDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .build();

        AnonymousProfileEntity expectedData = new AnonymousProfileEntity();
        expectedData.setProfile(mapper.writeValueAsString(profile));
        expectedData.setCreatedBy(IdRepoSecurityManager.getUser());

        assertEquals(expectedData.getProfile(), captureSavedProfile().getProfile());
    }


    @Test
    public void testBuildAndsaveProfileWithFileRefId() throws JsonProcessingException, IdRepoAppException {
        // Mock the ObjectStoreHelper to return decoded CBEFF bytes
        when(objectStoreHelper.getBiometricObject(any(), any()))
                .thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));

        // Call the method under test
        anonymousProfileHelper
                .setRegId("1")
                .setNewUinData(identityData.getBytes())
                .setNewCbeff("12_12", "1234")
                .setOldCbeff("12_12", "1234")
                .setOldUinData(identityData.getBytes())
                .buildAndsaveProfile(false);

        // Prepare expected profile JSON
        IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
                .setFilterLanguage("eng")
                .setProcessName("Update")
                .setOldIdentity(identityData.getBytes())
                .setOldDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .setNewIdentity(identityData.getBytes())
                .setNewDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .build();

        String expectedProfileJson = mapper.writeValueAsString(profile);

        assertEquals(expectedProfileJson, captureSavedProfile().getProfile());

        // Context is detached when work is submitted.
        assertFalse(anonymousProfileHelper.isNewCbeffPresent());
        assertFalse(anonymousProfileHelper.isOldCbeffPresent());
    }


    @Test
    public void testBuildAndsaveProfileWithInvalidCbeff() throws JsonProcessingException, IdRepoAppException {
        // Mock ObjectStoreHelper to return invalid CBEFF bytes
        when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn("abcd".getBytes());

        // Call the method under test
        anonymousProfileHelper
                .setRegId("1")
                .setNewUinData(identityData.getBytes())
                .setNewCbeff("12_12", "1234")
                .setOldCbeff("12_12", "1234")
                .setOldUinData(identityData.getBytes())
                .buildAndsaveProfile(false);

        // Prepare expected profile JSON
        IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
                .setFilterLanguage("eng")
                .setProcessName("Update")
                .setOldIdentity(identityData.getBytes())
                .setOldDocuments(List.of()) // invalid CBEFF results in empty list
                .setNewIdentity(identityData.getBytes())
                .setNewDocuments(List.of())
                .build();

        String expectedProfileJson = mapper.writeValueAsString(profile);

        assertEquals(expectedProfileJson, captureSavedProfile().getProfile());
        assertFalse(anonymousProfileHelper.isNewCbeffPresent());
        assertFalse(anonymousProfileHelper.isOldCbeffPresent());
    }

    @Test
	public void testBuildAndsaveProfileWithNullRegId() throws JsonProcessingException, IdRepoAppException {
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn("abcd".getBytes());
		anonymousProfileHelper
				.setRegId(null)
				.setNewUinData(identityData.getBytes())
				.setNewCbeff("12_12", "1234")
				.setOldCbeff("12_12", "1234")
				.setOldUinData(identityData.getBytes())
				.buildAndsaveProfile(false);
		verifyNoInteractions(
				anonymousProfileExecutor, anonymousProfileRepo, channelInfoHelper);
	}

    @Test
    public void testDelayedNewProfileSurvivesNextRequest() throws Exception {
        assertDelayedProfileSurvivesNextRequest(false);
    }

    @Test
    public void testDelayedUpdateProfileSurvivesNextRequest() throws Exception {
        assertDelayedProfileSurvivesNextRequest(true);
    }

    private void assertDelayedProfileSurvivesNextRequest(boolean update)
            throws Exception {
        Queue<Runnable> pending = queueProfileWork();
        byte[] newData = identityData.getBytes(StandardCharsets.UTF_8);
        byte[] oldData = update ? newData.clone() : null;

        anonymousProfileHelper.setRegId("request-A")
                .setOldUinData(oldData)
                .setNewUinData(newData)
                .buildAndsaveProfile(false);

        assertEquals(1, pending.size());
        verifyNoInteractions(anonymousProfileRepo, channelInfoHelper);

        // Start another request before A's worker runs.
        anonymousProfileHelper.setRegId("request-B")
                .setNewUinData(identityData.replace("\"Male\"", "\"Female\"")
                        .getBytes(StandardCharsets.UTF_8));

        runOnWorker(pending.remove());

        IdentityIssuanceProfile saved = mapper.readValue(
                captureSavedProfile().getProfile(), IdentityIssuanceProfile.class);
        assertEquals(update ? "Update" : "New", saved.getProcessName());
        assertNotNull(saved.getNewProfile());

        IdentityIssuanceProfile expected = IdentityIssuanceProfile.builder()
                .setFilterLanguage("eng")
                .setProcessName(update ? "Update" : "New")
                .setOldIdentity(oldData)
                .setOldDocuments(List.of())
                .setNewIdentity(newData)
                .setNewDocuments(List.of())
                .build();

        assertEquals(expected.getNewProfile(), saved.getNewProfile());
        assertEquals(expected.getOldProfile(), saved.getOldProfile());
        verify(channelInfoHelper).updatePhoneChannelInfo(oldData, newData);
        verify(channelInfoHelper).updateEmailChannelInfo(oldData, newData);
    }

    @Test
    public void testConcurrentRequestThreadsKeepSeparateProfiles()
            throws Exception {
        Queue<Runnable> pending = queueProfileWork();
        byte[] dataA = identityData.getBytes(StandardCharsets.UTF_8);
        byte[] dataB = identityData.replace("\"Male\"", "\"Female\"")
                .getBytes(StandardCharsets.UTF_8);

        // A is staged while B runs on another request thread.
        anonymousProfileHelper.setRegId("request-A")
                .setOldUinData(dataA)
                .setNewUinData(dataA);

        runOnWorker(() -> anonymousProfileHelper.setRegId("request-B")
                .setNewUinData(dataB)
                .buildAndsaveProfile(false));

        anonymousProfileHelper.buildAndsaveProfile(false);

        assertEquals(2, pending.size());
        verifyNoInteractions(anonymousProfileRepo);

        // B queued first; execute both on worker threads.
        runOnWorker(pending.remove());
        runOnWorker(pending.remove());

        ArgumentCaptor<AnonymousProfileEntity> captor =
                ArgumentCaptor.forClass(AnonymousProfileEntity.class);
        verify(anonymousProfileRepo, times(2)).save(captor.capture());

        IdentityIssuanceProfile profileB = mapper.readValue(
                captor.getAllValues().get(0).getProfile(),
                IdentityIssuanceProfile.class);
        IdentityIssuanceProfile profileA = mapper.readValue(
                captor.getAllValues().get(1).getProfile(),
                IdentityIssuanceProfile.class);

        assertEquals("New", profileB.getProcessName());
        assertNull(profileB.getOldProfile());
        assertNotNull(profileB.getNewProfile());
        assertEquals("Update", profileA.getProcessName());
        assertNotNull(profileA.getOldProfile());
        assertNotNull(profileA.getNewProfile());
        assertFalse(profileA.getNewProfile().equals(profileB.getNewProfile()));

        verify(channelInfoHelper).updatePhoneChannelInfo(null, dataB);
        verify(channelInfoHelper).updatePhoneChannelInfo(dataA, dataA);
        verify(channelInfoHelper).updateEmailChannelInfo(null, dataB);
        verify(channelInfoHelper).updateEmailChannelInfo(dataA, dataA);
    }

    @Test
    public void testDraftClearsContextAndSkipsProfileCreation() {
        anonymousProfileHelper.setRegId("draft")
                .setNewUinData(identityData.getBytes(StandardCharsets.UTF_8))
                .setNewCbeff(cbeff)
                .buildAndsaveProfile(true);

        assertFalse(anonymousProfileHelper.isNewCbeffPresent());

        // Cleared draft data must not be reused.
        anonymousProfileHelper.buildAndsaveProfile(false);
        verifyNoInteractions(
                anonymousProfileExecutor, anonymousProfileRepo, channelInfoHelper);
    }

    @Test
    public void testMissingNewIdentitySkipsProfileCreation() {
        anonymousProfileHelper.setRegId("missing-identity")
                .buildAndsaveProfile(false);

        verifyNoInteractions(
                anonymousProfileExecutor, anonymousProfileRepo, channelInfoHelper);
    }

    private Queue<Runnable> queueProfileWork() {
        Queue<Runnable> pending = new ArrayDeque<>();
        doAnswer(invocation -> {
            pending.add(invocation.getArgument(0));
            return null;
        }).when(anonymousProfileExecutor).execute(any(Runnable.class));
        return pending;
    }

    private void runOnWorker(Runnable work) throws Exception {
        FutureTask<Void> task = new FutureTask<>(work, null);
        Thread worker = new Thread(task, "anonymous-profile-test-worker");
        worker.setDaemon(true);
        worker.start();
        task.get(5, TimeUnit.SECONDS);
    }

    private AnonymousProfileEntity captureSavedProfile() {
        ArgumentCaptor<AnonymousProfileEntity> captor =
                ArgumentCaptor.forClass(AnonymousProfileEntity.class);
        verify(anonymousProfileRepo).save(captor.capture());

        AnonymousProfileEntity saved = captor.getValue();
        assertNotNull(saved.getId());
        assertNotNull(saved.getCrDTimes());
        assertEquals(IdRepoSecurityManager.getUser(), saved.getCreatedBy());
        return saved;
    }
}
