package io.mosip.credential.request.generator.test.batch.config;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.credential.request.generator.batch.config.BatchConfiguration;
import io.mosip.credential.request.generator.batch.config.CredentialItemProcessor;
import io.mosip.credential.request.generator.batch.config.CredentialItemReProcessor;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.util.RestUtil;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class BatchConfigurationTest {

	@InjectMocks
	private BatchConfiguration batchConfiguration;

	/** The job builder factory. */
	@Mock
	public JobBuilderFactory jobBuilderFactory;

	/** The step builder factory. */
	@Mock
	public StepBuilderFactory stepBuilderFactory;

	/** The job launcher. */
	@Mock
	private JobLauncher jobLauncher;

	/** The crdential repo. */
	@Mock
	private CredentialRepositary<CredentialEntity, String> crdentialRepo;

	/** The credential process job. */
	@Mock
	private Job credentialProcessJob;

	/** The credential re process job. */
	@Mock
	private Job credentialReProcessJob;

	@Before
	public void before() {
	}

	@Test
	public void processJobTest() {
		batchConfiguration.processJob();
	}

	@Test
	public void reProcessJobTest() {
		batchConfiguration.reProcessJob();
	}

	@Test
	public void processorTest() {
		CredentialItemProcessor res = batchConfiguration.processor();
		assertNotNull(res);
	}

	@Test
	public void reProcessorTest() {
		CredentialItemReProcessor res = batchConfiguration.reProcessor();
		assertNotNull(res);
	}

	@Test
	public void getRestUtilTest() {
		RestUtil res = batchConfiguration.getRestUtil();
		assertNotNull(res);
	}

	@Test
	public void getTaskSchedulerTest() {
		ThreadPoolTaskScheduler res = batchConfiguration.getTaskScheduler();
		assertNotNull(res);
	}

	@Test
	public void asyncItemWriterTest() {
		AsyncItemWriter<CredentialEntity> res = batchConfiguration.asyncItemWriter();
		assertNotNull(res);
	}

	@Test
	public void asyncItemWReprocessWriterTest() {
		AsyncItemWriter<CredentialEntity> res = batchConfiguration.asyncItemWReprocessWriter();
		assertNotNull(res);
	}

	@Test
	public void reProcesswriterTest() {
		RepositoryItemWriter<CredentialEntity> res = batchConfiguration.reProcesswriter();
		assertNotNull(res);
	}

	@Test
	public void alterAnnotationTest() throws Exception {
		String res = batchConfiguration.alterAnnotation();
		assertEquals("", res);
	}

	@Test
	public void afterburnerModuleTest() {
		AfterburnerModule res = batchConfiguration.afterburnerModule();
		assertNotNull(res);
	}

}
