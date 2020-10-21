package io.mosip.credentialstore.test.util;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credentialstore.util.WebSubUtil;
import io.mosip.idrepository.core.dto.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class WebsubUtilTest {
	@Mock
	private PublisherClient<String, EventModel, HttpHeaders> pb;
	
	@InjectMocks
	WebSubUtil webSubUtil;
	

	@Test
	public void testPublishEventSuccess() throws WebSubClientException, IOException {
		webSubUtil.publishSuccess("12345", null);
	}

	}
