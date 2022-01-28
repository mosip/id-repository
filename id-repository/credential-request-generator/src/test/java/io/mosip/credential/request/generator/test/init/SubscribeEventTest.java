package io.mosip.credential.request.generator.test.init;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credential.request.generator.init.SubscribeEvent;
import io.mosip.credential.request.generator.integration.WebSubSubscriptionHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@Ignore
public class SubscribeEventTest {

	@InjectMocks
	private SubscribeEvent subscribeEvent;

	@Mock
	private ThreadPoolTaskScheduler taskScheduler;

	@Value("${subscription-delay-secs:60000}")
	private int taskSubsctiptionDelay;

	@Mock
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	/** The Constant BIOMETRICS. */
	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String SUBSCIRBEEVENT = "SubscribeEvent";

	private static final Logger LOGGER = IdRepoLogger.getLogger(SubscribeEvent.class);

	@Before
	public void before() {
		ReflectionTestUtils.setField(subscribeEvent, "taskSubsctiptionDelay", 60000);
	}

}
