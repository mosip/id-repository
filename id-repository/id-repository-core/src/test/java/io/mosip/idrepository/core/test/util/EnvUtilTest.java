package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.util.EnvUtil;

/**
 * @author Manoj SP
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvUtilTest {

	@InjectMocks
	private EnvUtil envUtil;
	
	@Mock
	private ConfigurableEnvironment env;
	
	@Test
	public void testMerge() {
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("property", "value");
		doNothing().when(env).merge(mockEnv);
		envUtil.merge(mockEnv);
		verify(env, times(1)).merge(mockEnv);
	}
	
	@Test
	public void testInitCredentialRequestGeneratorServiceProperties() {
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("spring.application.name", "credential-request");
		ReflectionTestUtils.setField(envUtil, "env", mockEnv);
		envUtil.init();
		assertEquals("credential-request", envUtil.getProperty("spring.application.name"));
	}
	
	@Test
	public void testInitCredentialServiceProperties() {
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("spring.application.name", "credential-service");
		ReflectionTestUtils.setField(envUtil, "env", mockEnv);
		envUtil.init();
		assertEquals("credential-service", envUtil.getProperty("spring.application.name"));
	}
}
