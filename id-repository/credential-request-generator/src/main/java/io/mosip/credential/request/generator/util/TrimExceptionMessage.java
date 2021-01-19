package io.mosip.credential.request.generator.util;

public class TrimExceptionMessage {
	private static final int MESSAGE_LENGTH = 400;

	public String trimExceptionMessage(String exceptionMessage) {
		return exceptionMessage.substring(0, Math.min(exceptionMessage.length(), MESSAGE_LENGTH));

	}
}
