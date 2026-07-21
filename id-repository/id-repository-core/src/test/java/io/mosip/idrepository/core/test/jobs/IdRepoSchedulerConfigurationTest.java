package io.mosip.idrepository.core.test.jobs;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.mosip.idrepository.core.jobs.IdRepoSchedulerConfiguration;

public class IdRepoSchedulerConfigurationTest {

	@Test
	public void configurationClassLoads() {
		assertNotNull(new IdRepoSchedulerConfiguration());
	}
}
