package io.mosip.idrepository.core.constant;

/**
 * Lifecycle states for rows in the {@code idrepo.handle} table.
 *
 * <p>
 * Handles are unique by {@code handle_hash} regardless of status. Credential issuance
 * uses {@link #ACTIVATED} rows only. Identity update marks removed handles {@link #DELETE};
 * after IDA {@code REMOVE_ID} they become {@link #DELETE_REQUESTED}.
 * </p>
 *
 * @see io.mosip.idrepository.core.entity.Handle
 * @see io.mosip.idrepository.core.repository.HandleRepo
 */
public enum HandleStatusLifecycle {

	/** Handle is in use and eligible for retrieve-by-handle and credential issuance. */
	ACTIVATED,

	/** Handle was removed from {@code selectedHandles}; pending IDA {@code REMOVE_ID}. */
	DELETE,

	/** IDA has been notified to drop the handle; row is retained for uniqueness. */
	DELETE_REQUESTED;

	/**
	 * Resolves a persisted status string to this enum.
	 *
	 * @param status value stored on {@code idrepo.handle.status}
	 * @return matching constant, or {@code null} when the string is unknown
	 */
	public static HandleStatusLifecycle getHandleStatus(String status) {
		for (HandleStatusLifecycle handleStatus : HandleStatusLifecycle.values()) {
			if (handleStatus.name().equals(status)) {
				return handleStatus;
			}
		}
		return null;
	}
}
