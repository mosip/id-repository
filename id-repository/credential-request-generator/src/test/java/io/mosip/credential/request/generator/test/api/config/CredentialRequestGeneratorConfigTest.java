package io.mosip.credential.request.generator.test.api.config;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credential.request.generator.api.config.CredentialRequestGeneratorConfig;
import io.mosip.credential.request.generator.api.config.OpenApiProperties;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.idrepository.core.builder.RestRequestBuilder;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class CredentialRequestGeneratorConfigTest {

	@InjectMocks
	private CredentialRequestGeneratorConfig credentialRequestGeneratorConfig;

	@Mock
	@Qualifier("restUtil")
	private RestUtil restUtil;

	@Mock
	private OpenApiProperties openApiProperties;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void getRestRequestBuilder() {
		RestRequestBuilder response = credentialRequestGeneratorConfig.getRestRequestBuilder();
		assertNotNull(response);
	}

}
