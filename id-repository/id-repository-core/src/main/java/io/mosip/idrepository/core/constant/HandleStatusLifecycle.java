package io.mosip.idrepository.core.constant;

/**
 * @author Ritik Jain
 *
 */
public enum HandleStatusLifecycle {
	ACTIVATED, DELETE, DELETE_REQUESTED;

	public static HandleStatusLifecycle getHandleStatus(String status) {
		for (HandleStatusLifecycle handleStatus : HandleStatusLifecycle.values()) {
			if (handleStatus.name().equals(status)) {
				return handleStatus;
			}
		}
        return null;
    }
}
