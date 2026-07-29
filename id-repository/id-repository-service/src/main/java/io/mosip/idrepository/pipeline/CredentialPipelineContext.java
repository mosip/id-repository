package io.mosip.idrepository.pipeline;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.mosip.idrepository.core.dto.IdResponseDTO;

/**
 * Thread-local context for a single credential-status processing cycle.
 * <p>
 * Carries the plain and encrypted UIN after one decrypt from
 * {@code credential_request_status}, so downstream steps avoid re-encrypting the same
 * identifier when creating per-partner status rows.
 * </p>
 * <p>
 * Also holds a shared identity-response cache for the cycle so parallel partner
 * credential issuance reuses one retrieve (per id + extraction-format key) instead of
 * N full identity + BioSDK round-trips.
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

	/**
	 * Attaches an existing {@link State} to the current thread (e.g. worker threads in a
	 * parallel partner fan-out sharing one identity cache).
	 *
	 * @param state pipeline state; ignored if {@code null}
	 */
	public static void attach(State state) {
		if (state != null) {
			CONTEXT.set(state);
		}
	}

	/** @return current pipeline state, or {@code null} if unset */
	public static State get() {
		return CONTEXT.get();
	}

	/** Clears thread-local state; must be called in {@code finally} after processing. */
	public static void clear() {
		CONTEXT.remove();
	}

	/**
	 * Snapshot for one credential issuance cycle.
	 * <p>
	 * UIN fields are immutable; {@link #getIdentityCache()} is concurrent and shared across
	 * worker threads that {@link #attach(State)} the same instance.
	 * </p>
	 */
	public static final class State {

		private final String plainIndividualId;
		private final String encryptedIndividualId;
		private final String triggerAction;
		private final ConcurrentMap<String, IdResponseDTO> identityCache = new ConcurrentHashMap<>();

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

		/**
		 * Per-cycle identity responses keyed by id + extraction-format fingerprint.
		 *
		 * @return shared concurrent map (never {@code null})
		 */
		public ConcurrentMap<String, IdResponseDTO> getIdentityCache() {
			return identityCache;
		}
	}
}
