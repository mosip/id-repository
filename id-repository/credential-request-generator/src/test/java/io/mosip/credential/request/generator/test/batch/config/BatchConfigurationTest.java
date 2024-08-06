package io.mosip.credential.request.generator.test.batch.config;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.credential.request.generator.batch.config.BatchConfiguration;
import io.mosip.credential.request.generator.batch.config.CredentialItemReprocessTasklet;
import io.mosip.credential.request.generator.batch.config.CredentialItemTasklet;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.idrepository.core.util.EnvUtil;

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



	/** The credential process job. */
	@Mock
	private Job credentialProcessJob;

	/** The credential re process job. */
	@Mock
	private Job credentialReProcessJob;
	
	@Mock
	public CredentialItemTasklet credentialItemTasklet;

	@Mock
	public CredentialItemReprocessTasklet credentialItemReprocessTasklet;

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
