package io.mosip.idrepository.pipeline;

/**
 * Thread-local context for a single credential-status processing cycle.
 * <p>
 * Carries the plain and encrypted UIN after one decrypt from
 * {@code credential_request_status}, so downstream steps avoid re-encrypting the same
 * identifier when creating per-partner status rows.
 * </p>
 */
public final class CredentialPipelineContext {

	private static final ThreadLocal<State> CONTEXT = new ThreadLocal<>();

	private CredentialPipelineContext() {
	}

	/**
	 * Binds plain/encrypted UIN and optional trigger action for the current thread.
	 *
	 * @param plainIndividualId     decrypted UIN
	 * @param encryptedIndividualId encrypted UIN as stored in {@code credential_request_status}
	 * @param triggerAction         CREATE or UPDATE trigger from the status row
	 */
	public static void set(String plainIndividualId, String encryptedIndividualId, String triggerAction) {
		CONTEXT.set(new State(plainIndividualId, encryptedIndividualId, triggerAction));
	}

	/** @return current pipeline state, or {@code null} if unset */
	public static State get() {
		return CONTEXT.get();
	}

	/** Clears thread-local state; must be called in {@code finally} after processing. */
	public static void clear() {
		CONTEXT.remove();
	}

	/** Immutable snapshot for one credential issuance cycle. */
	public static final class State {

		private final String plainIndividualId;
		private final String encryptedIndividualId;
		private final String triggerAction;

		public State(String plainIndividualId, String encryptedIndividualId, String triggerAction) {
			this.plainIndividualId = plainIndividualId;
			this.encryptedIndividualId = encryptedIndividualId;
			this.triggerAction = triggerAction;
		}

		public String getPlainIndividualId() {
			return plainIndividualId;
		}

		public String getEncryptedIndividualId() {
			return encryptedIndividualId;
		}

		public String getTriggerAction() {
			return triggerAction;
		}
	}
}
