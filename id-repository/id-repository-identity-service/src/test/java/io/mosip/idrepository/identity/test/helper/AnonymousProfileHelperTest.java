package io.mosip.idrepository.identity.test.helper;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import io.mosip.idrepository.identity.helper.AnonymousProfileHelper;
import io.mosip.idrepository.identity.helper.ChannelInfoHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.repository.AnonymousProfileRepo;
import io.mosip.kernel.core.util.CryptoUtil;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
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
	private String cbeff;
	private String identityData;
	private IdentityMapping identityMapping;

	@Before
	public void init() throws Exception {
		ReflectionTestUtils.setField(anonymousProfileHelper, "mapper", mapper);
		ReflectionTestUtils.setField(anonymousProfileHelper, "identityMappingJson", "dummy-url");
		ReflectionTestUtils.setField(anonymousProfileHelper, "anonymousProfileExecutor",
				(Executor) Runnable::run);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		cbeff = IOUtils.toString(this.getClass().getClassLoader()
				.getResourceAsStream("test-cbeff.xml"), StandardCharsets.UTF_8);
		identityData = IOUtils.toString(this.getClass().getClassLoader()
				.getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		identityMapping = mapper.readValue(IOUtils.toString(this.getClass().getClassLoader()
						.getResourceAsStream("identity-mapping.json"), StandardCharsets.UTF_8),
				IdentityMapping.class);
		IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu/MM/dd");
	}

	/** Captures the saved AnonymousProfileEntity and returns its profile JSON as a JsonNode. */
	private JsonNode captureAndReadProfile(int expectedCalls) throws JsonProcessingException {
		ArgumentCaptor<AnonymousProfileEntity> captor = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo, times(expectedCalls)).save(captor.capture());
		return mapper.readTree(captor.getValue().getProfile());
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
		JsonNode profile = captureAndReadProfile(1);
		assertEquals("Update", profile.get("processName").asText());
		assertFalse("newProfile should not be null", profile.get("newProfile").isNull());
		assertFalse("oldProfile should not be null", profile.get("oldProfile").isNull());
	}

	@Test
	public void testBuildAndsaveProfileWithFileRefId() throws Exception {
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any()))
				.thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		anonymousProfileHelper
				.setRegId("1")
				.setNewUinData(identityData.getBytes())
				.setOldUinData(identityData.getBytes())
				.setNewCbeff("12_12", "1234")
				.setOldCbeff("12_12", "1234")
				.buildAndsaveProfile(false);
		JsonNode profile = captureAndReadProfile(1);
		assertFalse("newProfile should not be null", profile.get("newProfile").isNull());
		assertFalse("oldProfile should not be null", profile.get("oldProfile").isNull());
	}

	@Test
	public void testBuildAndsaveProfileWithInvalidCbeff() throws Exception {
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any()))
				.thenReturn("abcd".getBytes());
		anonymousProfileHelper
				.setRegId("1")
				.setNewUinData(identityData.getBytes())
				.setOldUinData(identityData.getBytes())
				.setNewCbeff("12_12", "1234")
				.setOldCbeff("12_12", "1234")
				.buildAndsaveProfile(false);
		JsonNode profile = captureAndReadProfile(1);
		assertFalse("newProfile should not be null", profile.get("newProfile").isNull());
		assertFalse("oldProfile should not be null", profile.get("oldProfile").isNull());
	}

	@Test
	public void testBuildAndsaveProfileWithNullRegId() {
		anonymousProfileHelper
				.setRegId(null)
				.setNewUinData(identityData.getBytes())
				.buildAndsaveProfile(false);
		verify(anonymousProfileRepo, never()).save(Mockito.any());
	}

	@Test
	public void testNoDataInContext() {
		anonymousProfileHelper.buildAndsaveProfile(false);
		verify(anonymousProfileRepo, never()).save(Mockito.any());
	}

	@Test
	public void testRaceConditionFixed() throws Exception {
		// Request A sets its data
		anonymousProfileHelper
				.setRegId("regId-AAA")
				.setNewUinData(identityData.getBytes())
				.setNewCbeff(cbeff);
		// Request B interferes — resets the ThreadLocal for this thread
		anonymousProfileHelper.setRegId("regId-BBB");
		// Request A re-establishes its context and completes
		anonymousProfileHelper
				.setRegId("regId-AAA")
				.setNewUinData(identityData.getBytes())
				.setNewCbeff(cbeff);
		anonymousProfileHelper.buildAndsaveProfile(false);
		JsonNode saved = captureAndReadProfile(1);
		assertEquals("New", saved.get("processName").asText());
		assertFalse("newProfile should not be null", saved.get("newProfile").isNull());
		assertTrue("oldProfile should be null (no old identity)", saved.get("oldProfile").isNull());
	}

	@Test
	public void testAsyncProfileBuildRetainsContextAfterNextRequestStarts() throws Exception {
		CountDownLatch allowWorkerToRun = new CountDownLatch(1);
		ExecutorService worker = Executors.newSingleThreadExecutor();
		Executor gatedExecutor = task -> worker.execute(() -> {
			try {
				allowWorkerToRun.await();
				task.run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		ReflectionTestUtils.setField(anonymousProfileHelper, "anonymousProfileExecutor", gatedExecutor);

		try {
			anonymousProfileHelper
					.setRegId("regId-first")
					.setNewUinData(identityData.getBytes());
			anonymousProfileHelper.buildAndsaveProfile(false);

			// The next request must not affect the profile already queued above.
			anonymousProfileHelper.setRegId("regId-second");
			allowWorkerToRun.countDown();
			worker.shutdown();
			assertTrue(worker.awaitTermination(30, TimeUnit.SECONDS));

			JsonNode profile = captureAndReadProfile(1);
			assertEquals("New", profile.get("processName").asText());
			assertFalse("newProfile must survive the async handoff",
					profile.get("newProfile").isNull());
		} finally {
			allowWorkerToRun.countDown();
			worker.shutdownNow();
		}
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
			// Simulate interference
			anonymousProfileHelper.setRegId("interfering-regId-" + (i + 1));
			// Restore and complete
			anonymousProfileHelper
					.setRegId(regId)
					.setNewUinData(identityData.getBytes())
					.setNewCbeff(cbeff)
					.buildAndsaveProfile(false);
			ArgumentCaptor<AnonymousProfileEntity> captor = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
			verify(anonymousProfileRepo, times(i + 1)).save(captor.capture());
			JsonNode saved = mapper.readTree(captor.getAllValues().get(i).getProfile());
			if (!saved.get("newProfile").isNull() && "New".equals(saved.get("processName").asText())) {
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
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any()))
				.thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
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
		verify(anonymousProfileRepo, times(threadCount * iterationsPerThread)).save(Mockito.any());
	}
}