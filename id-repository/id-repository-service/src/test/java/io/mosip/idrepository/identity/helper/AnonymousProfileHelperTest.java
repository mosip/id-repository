package io.mosip.idrepository.identity.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.test.support.TestEnvSupport;
import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import io.mosip.idrepository.identity.repository.AnonymousProfileRepo;
import io.mosip.kernel.core.util.CryptoUtil;

@RunWith(MockitoJUnitRunner.class)
public class AnonymousProfileHelperTest {

	@Spy
	@InjectMocks
	private AnonymousProfileHelper anonymousProfileHelper;

	@Mock
	private AnonymousProfileRepo anonymousProfileRepo;

	private ObjectMapper mapper;

	@Mock
	private ObjectStoreHelper objectStoreHelper;

	@Mock
	private ChannelInfoHelper channelInfoHelper;

	private String cbeff;
	private String identityData;
	private IdentityMapping identityMapping;

	@Before
	public void init() throws Exception {
		TestEnvSupport.initEnvUtil(TestEnvSupport.loadTestEnvironment());
		mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		identityData = IOUtils.toString(this.getClass().getClassLoader()
				.getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		identityMapping = mapper.readValue(IOUtils.toString(this.getClass().getClassLoader()
				.getResourceAsStream("identity-mapping.json"), StandardCharsets.UTF_8),
				IdentityMapping.class);
		byte[] cbeffBytes = IOUtils.toString(this.getClass().getClassLoader()
				.getResourceAsStream("test-cbeff.xml"), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
		cbeff = CryptoUtil.encodeToURLSafeBase64(cbeffBytes);
		IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		IdentityIssuanceProfileBuilder.setDateFormat(EnvUtil.getIovDateFormat());
		ReflectionTestUtils.setField(anonymousProfileHelper, "mapper", mapper);
		// Run async handoff inline so unit tests can verify save() without a Spring executor.
		ReflectionTestUtils.setField(anonymousProfileHelper, "anonymousProfileExecutor",
				(java.util.concurrent.Executor) Runnable::run);
		ReflectionTestUtils.setField(anonymousProfileHelper, "identityMappingJson", "dummy-url");
	}

	@Test
	public void testBuildAndsaveProfile() throws JsonProcessingException {
		anonymousProfileHelper
				.setRegId("1")
				.setNewUinData(identityData.getBytes())
				.setOldUinData(identityData.getBytes())
				.setOldCbeff(cbeff)
				.setNewCbeff(cbeff)
				.buildAndsaveProfile(false);

		ArgumentCaptor<AnonymousProfileEntity> profileCaptor = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo, times(1)).save(profileCaptor.capture());
		IdentityIssuanceProfile actual = mapper.readValue(profileCaptor.getValue().getProfile(),
				IdentityIssuanceProfile.class);
		assertEquals("Update", actual.getProcessName());
		assertNotNull("newProfile should not be null", actual.getNewProfile());
		assertNotNull("oldProfile should not be null", actual.getOldProfile());
	}

	@Test
	public void testBuildAndsaveProfileWithFileRefId() throws Exception {
		when(objectStoreHelper.getBiometricObject(any(), any()))
				.thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		anonymousProfileHelper
				.setRegId("1")
				.setNewUinData(identityData.getBytes())
				.setOldUinData(identityData.getBytes())
				.setNewCbeff("12_12", "1234")
				.setOldCbeff("12_12", "1234")
				.buildAndsaveProfile(false);

		ArgumentCaptor<AnonymousProfileEntity> profileCaptor = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo, times(1)).save(profileCaptor.capture());
		IdentityIssuanceProfile profile = mapper.readValue(profileCaptor.getValue().getProfile(),
				IdentityIssuanceProfile.class);
		assertNotNull(profile.getNewProfile());
		assertNotNull(profile.getOldProfile());
	}

	@Test
	public void testBuildAndsaveProfileWithInvalidCbeff() throws Exception {
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn("abcd".getBytes());
		anonymousProfileHelper
				.setRegId("1")
				.setNewUinData(identityData.getBytes())
				.setOldUinData(identityData.getBytes())
				.setNewCbeff("12_12", "1234")
				.setOldCbeff("12_12", "1234")
				.buildAndsaveProfile(false);

		ArgumentCaptor<AnonymousProfileEntity> profileCaptor = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo, times(1)).save(profileCaptor.capture());
		IdentityIssuanceProfile profile = mapper.readValue(profileCaptor.getValue().getProfile(),
				IdentityIssuanceProfile.class);
		assertNotNull(profile.getNewProfile());
		assertNotNull(profile.getOldProfile());
	}

	@Test
	public void testBuildAndsaveProfileWithNullRegId() {
		anonymousProfileHelper
				.setRegId(null)
				.setNewUinData(identityData.getBytes())
				.buildAndsaveProfile(false);
		verify(anonymousProfileRepo, never()).save(any());
	}

	@Test
	public void testNoDataInContext() {
		anonymousProfileHelper.buildAndsaveProfile(false);
		verify(anonymousProfileRepo, never()).save(any());
	}

	@Test
	public void testRaceConditionFixed() throws Exception {
		anonymousProfileHelper
				.setRegId("regId-AAA")
				.setNewUinData(identityData.getBytes())
				.setNewCbeff(cbeff);
		anonymousProfileHelper.setRegId("regId-BBB");
		anonymousProfileHelper
				.setRegId("regId-AAA")
				.setNewUinData(identityData.getBytes())
				.setNewCbeff(cbeff);
		anonymousProfileHelper.buildAndsaveProfile(false);

		ArgumentCaptor<AnonymousProfileEntity> profileCaptor = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo, times(1)).save(profileCaptor.capture());
		IdentityIssuanceProfile saved = mapper.readValue(profileCaptor.getValue().getProfile(),
				IdentityIssuanceProfile.class);
		assertEquals("New", saved.getProcessName());
		assertNotNull("newProfile should not be null after fix", saved.getNewProfile());
		assertNull("oldProfile should be null for New process", saved.getOldProfile());
	}

	@Test
	public void testRaceConditionUnderLoad_100Times() throws Exception {
		int iterations = 100;
		int successCount = 0;
		for (int i = 0; i < iterations; i++) {
			String regId = "regId-" + i;
			anonymousProfileHelper
					.setRegId(regId)
					.setNewUinData(identityData.getBytes())
					.setNewCbeff(cbeff);
			anonymousProfileHelper.setRegId("interfering-regId-" + (i + 1));
			anonymousProfileHelper
					.setRegId(regId)
					.setNewUinData(identityData.getBytes())
					.setNewCbeff(cbeff)
					.buildAndsaveProfile(false);

			ArgumentCaptor<AnonymousProfileEntity> profileCaptor = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
			verify(anonymousProfileRepo, times(i + 1)).save(profileCaptor.capture());
			IdentityIssuanceProfile saved = mapper.readValue(
					profileCaptor.getAllValues().get(i).getProfile(), IdentityIssuanceProfile.class);
			if (saved.getNewProfile() != null && "New".equals(saved.getProcessName())) {
				successCount++;
			}
		}
		assertEquals("All 100 iterations should succeed without null profiles", iterations, successCount);
	}

	@Test
	public void testRaceConditionWithMultiThreading() throws Exception {
		int threadCount = 50;
		int iterationsPerThread = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		List<Future<Void>> futures = new ArrayList<>();

		for (int t = 0; t < threadCount; t++) {
			final int threadId = t;
			Callable<Void> task = () -> {
				for (int i = 0; i < iterationsPerThread; i++) {
					String regId = "reg-" + threadId + "-" + i;
					anonymousProfileHelper
							.setRegId(regId)
							.setNewUinData(identityData.getBytes())
							.setNewCbeff(cbeff);
					anonymousProfileHelper.setRegId("interfere-" + threadId);
					anonymousProfileHelper
							.setRegId(regId)
							.setNewUinData(identityData.getBytes())
							.setNewCbeff(cbeff)
							.buildAndsaveProfile(false);
				}
				return null;
			};
			futures.add(executor.submit(task));
		}

		for (Future<Void> future : futures) {
			future.get(30, TimeUnit.SECONDS);
		}
		executor.shutdown();
		executor.awaitTermination(30, TimeUnit.SECONDS);

		int expectedCalls = threadCount * iterationsPerThread;
		verify(anonymousProfileRepo, times(expectedCalls)).save(any(AnonymousProfileEntity.class));
	}
}
