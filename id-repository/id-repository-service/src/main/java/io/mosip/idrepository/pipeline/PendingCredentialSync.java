package io.mosip.idrepository.pipeline;

import java.time.LocalDateTime;

/**
 * Queues sync credential work to run <em>after</em> the identity DB transaction fully ends.
 * <p>
 * Spring invokes {@code TransactionSynchronization.afterCommit} <strong>before</strong>
 * releasing the JDBC connection back to Hikari. Running keymanager/datashare work there
 * holds an {@code idrepo-pool} connection for the full credential latency (~15–30s) and
 * exhausts the pool under concurrent add/update/publish (see Hikari
 * {@code Connection is not available, request timed out}).
 * </p>
 * <p>
 * Identity code enqueues here inside the TX; callers outside the TX
 * ({@code IdRepoProxyServiceImpl}, draft publish) drain via
 * {@link io.mosip.idrepository.manager.CredentialStatusManager#runPendingAfterIdentityCommit()}.
 * </p>
 */
public final class PendingCredentialSync {

	private static final ThreadLocal<Task> PENDING = new ThreadLocal<>();

	private PendingCredentialSync() {
	}

	/** Replace any pending task for this thread (one identity mutation → one sync). */
	public static void enqueue(String plainUin, String encryptedUin, String uinStatus,
			LocalDateTime expiryTimestamp, boolean isUpdate, String requestId) {
		PENDING.set(new Task(plainUin, encryptedUin, uinStatus, expiryTimestamp, isUpdate, requestId));
	}

	/** @return pending task, or {@code null} */
	public static Task poll() {
		Task task = PENDING.get();
		PENDING.remove();
		return task;
	}

	/** Drop without running (e.g. identity TX rolled back). */
	public static void clear() {
		PENDING.remove();
	}

	/** Parameters for {@code processSynchronouslyAfterIssueCredential}. */
	public static final class Task {
		private final String plainUin;
		private final String encryptedUin;
		private final String uinStatus;
		private final LocalDateTime expiryTimestamp;
		private final boolean isUpdate;
		private final String requestId;

		public Task(String plainUin, String encryptedUin, String uinStatus, LocalDateTime expiryTimestamp,
				boolean isUpdate, String requestId) {
			this.plainUin = plainUin;
			this.encryptedUin = encryptedUin;
			this.uinStatus = uinStatus;
			this.expiryTimestamp = expiryTimestamp;
			this.isUpdate = isUpdate;
			this.requestId = requestId;
		}

		public String getPlainUin() {
			return plainUin;
		}

		public String getEncryptedUin() {
			return encryptedUin;
		}

		public String getUinStatus() {
			return uinStatus;
		}

		public LocalDateTime getExpiryTimestamp() {
			return expiryTimestamp;
		}

		public boolean isUpdate() {
			return isUpdate;
		}

		public String getRequestId() {
			return requestId;
		}
	}
}
