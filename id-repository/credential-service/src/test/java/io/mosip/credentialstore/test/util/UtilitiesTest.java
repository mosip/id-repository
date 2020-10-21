package io.mosip.credentialstore.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.PolicyException;
import io.mosip.credentialstore.util.Utilities;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class UtilitiesTest {

	String credentialTypeJson;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private Utilities utilities;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws IOException {

		ClassLoader classLoader = getClass().getClassLoader();
		File credentialJson = new File(classLoader.getResource("CredentialType.json").getFile());
		InputStream is = new FileInputStream(credentialJson);
		credentialTypeJson = IOUtils.toString(is, "UTF-8");
		Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.any())).thenReturn(credentialTypeJson);
	}

	@Test
	public void getCredentialTypesSuccessTest() throws IOException, PolicyException, ApiNotAccessibleException {
		utilities.getTypes("", "");
	}
}
