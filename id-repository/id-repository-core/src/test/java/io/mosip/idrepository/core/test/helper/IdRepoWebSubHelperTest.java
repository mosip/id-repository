package io.mosip.idrepository.core.test.helper;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.constants.WebSubClientErrorCode;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

/**
 * 
 * @author Loganathan S
 *
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class IdRepoWebSubHelperTest {

	@Test
	public void testTryRegisterCache() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		idRepoWebSubHelper.tryRegisteringTopic("topic");
		// Throw the error for the second time to make sure if it is cached in first
		// call
		Mockito.lenient().doThrow(new RuntimeException()).when(publisherClient).registerTopic(Mockito.any(),
				Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(1, field.size());
	}

	@Test
	public void testTryRegisterCache_withRegistrationException() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		// Throw the error for the second time to make sure if it is cached in first
		// call
		Mockito.lenient()
				.doThrow(new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(),
						WebSubClientErrorCode.REGISTER_ERROR.getErrorMessage()))
				.when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(1, field.size());
	}

	@Test
	public void testTryRegisterCache_withNonRegistrationWebsubException() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		// Throw the error for the first time only to make sure if it is cached in
		// second call
		Mockito.lenient()
				.doThrow(new WebSubClientException(WebSubClientErrorCode.AUTHENTTICATED_CONTENT_ERROR.getErrorCode(),
						WebSubClientErrorCode.AUTHENTTICATED_CONTENT_ERROR.getErrorMessage()))
				.when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(0, field.size());

		Mockito.lenient().doNothing().when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		assertEquals(1, field.size());
	}

	@Test
	public void testTryRegisterCache_withNonWebsubException() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		// Throw the error for the first time only to make sure if it is cached in
		// second call
		Mockito.lenient()
				.doThrow(new WebSubClientException(WebSubClientErrorCode.AUTHENTTICATED_CONTENT_ERROR.getErrorCode(),
						WebSubClientErrorCode.AUTHENTTICATED_CONTENT_ERROR.getErrorMessage()))
				.when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(0, field.size());

		Mockito.lenient().doNothing().when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		assertEquals(1, field.size());

	}

}
