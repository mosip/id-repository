package io.mosip.idrepository.credential.request.util;

/**
 * Truncates exception messages before persisting to {@code credential_transaction.status_comment}.
 * <p>
 * Prevents oversized DB column values when HTTP error bodies are stored on FAILED rows.
 * </p>
 */
public class TrimExceptionMessage {

	/** Maximum characters stored in {@code status_comment}. */
	private static final int MESSAGE_LENGTH = 400;

	/**
	 * Truncates the message to {@link #MESSAGE_LENGTH} characters.
	 *
	 * @param exceptionMessage raw error text from HTTP or stack trace
	 * @return truncated message safe for DB persistence
	 */
	public String trimExceptionMessage(String exceptionMessage) {
		return exceptionMessage.substring(0, Math.min(exceptionMessage.length(), MESSAGE_LENGTH));
	}
}
