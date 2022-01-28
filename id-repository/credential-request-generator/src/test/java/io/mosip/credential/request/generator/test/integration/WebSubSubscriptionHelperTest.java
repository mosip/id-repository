package io.mosip.credential.request.generator.test.integration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credential.request.generator.integration.WebSubSubscriptionHelper;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class WebSubSubscriptionHelperTest {

	@InjectMocks
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	@Mock
	SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> sb;

	@Value("${" + IdRepoConstants.WEB_SUB_HUB_URL + "}")
	private String webSubHubUrl;

	@Value("${WEBSUBSECRET}")
	private String webSubSecret;

	@Value("${CALLBACKURL}")
	private String callBackUrl;

	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	private String partnerhuburl;

	@Mock
	private PublisherClient<String, EventModel, HttpHeaders> pb;

	@Before
	public void before() {
		ReflectionTestUtils.setField(webSubSubscriptionHelper, "webSubHubUrl",
				"${" + IdRepoConstants.WEB_SUB_HUB_URL + "}");
		ReflectionTestUtils.setField(webSubSubscriptionHelper, "webSubSecret", "${WEBSUBSECRET}");
		ReflectionTestUtils.setField(webSubSubscriptionHelper, "callBackUrl", "${CALLBACKURL}");
		ReflectionTestUtils.setField(webSubSubscriptionHelper, "partnerhuburl",
				"${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}");
	}

	@Test
	public void initSubsriptionsTest() {
		webSubSubscriptionHelper.initSubsriptions();
	}

	@Test
	public void registerTopicTest() {
		webSubSubscriptionHelper.registerTopic();
	}

}
