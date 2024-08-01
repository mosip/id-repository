package io.mosip.credential.request.generator.test.batch.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.credential.request.generator.batch.config.BatchConfiguration;
import io.mosip.credential.request.generator.batch.config.CredentialItemReprocessTasklet;
import io.mosip.credential.request.generator.batch.config.CredentialItemTasklet;
import io.mosip.credential.request.generator.util.RestUtil;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class BatchConfigurationTest {

	@InjectMocks
	private BatchConfiguration batchConfiguration;

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
