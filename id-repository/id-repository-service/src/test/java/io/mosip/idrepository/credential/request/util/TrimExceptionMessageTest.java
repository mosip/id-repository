package io.mosip.idrepository.credential.request.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TrimExceptionMessageTest {

	private final TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();

	@Test
	public void trimShortMessageUnchanged() {
		String message = "short error";
		assertEquals(message, trimExceptionMessage.trimExceptionMessage(message));
	}

	@Test
	public void trimLongMessageToMaxLength() {
		String message = "x".repeat(500);
		String trimmed = trimExceptionMessage.trimExceptionMessage(message);
		assertEquals(400, trimmed.length());
		assertEquals("x".repeat(400), trimmed);
	}

	@Test
	public void trimMessageExactlyAtMaxLength() {
		String message = "y".repeat(400);
		assertEquals(message, trimExceptionMessage.trimExceptionMessage(message));
	}
}
