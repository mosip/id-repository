package io.mosip.credential.request.generator.init;

import io.mosip.credential.request.generator.integration.WebSubSubscriptionHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;


@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@RunWith(SpringRunner.class)
public class CredentialInitializerTest {

    @InjectMocks
    private CredentialInstializer credentialInstializer;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private WebSubSubscriptionHelper webSubSubscriptionHelper;

    /**
     * This class tests the onApplicationEvent method
     */
    @Test
    public void onApplicationEventTest(){
        SpringApplication application = new SpringApplication();
        ApplicationReadyEvent event = new ApplicationReadyEvent(application, new String[0], null);
        credentialInstializer.onApplicationEvent(event);

        ReflectionTestUtils.setField(credentialInstializer, "reSubscriptionDelaySecs", 2);
        credentialInstializer.onApplicationEvent(event);
    }

    /**
     * This class tests the retrySubscriptions method
     */
    @Test
    public void retrySubscriptionsTest(){
        ReflectionTestUtils.invokeMethod(credentialInstializer, "retrySubscriptions");

        ReflectionTestUtils.setField(credentialInstializer, "webSubSubscriptionHelper", null);
        ReflectionTestUtils.invokeMethod(credentialInstializer, "retrySubscriptions");
    }

    /**
     * This class tests the retrySubscriptions method
     */
    @Test
    public void retrySubscriptionsExceptionTest(){
        ReflectionTestUtils.setField(credentialInstializer, "webSubSubscriptionHelper", null);
        ReflectionTestUtils.invokeMethod(credentialInstializer, "retrySubscriptions");
    }
}
