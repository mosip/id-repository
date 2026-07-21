package io.mosip.idrepository.core.spi;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Thread-local routing datasource that selects a physical database by shard name.
 * <p>
 * Extends Spring's {@link AbstractRoutingDataSource} and uses a
 * {@link ThreadLocal} to hold the current shard key for the calling thread.
 * {@link ShardResolver} sets the shard before repository access; callers must
 * invoke {@link #resetShardConfig()} in a {@code finally} block to avoid
 * leaking shard context across pooled threads.
 * </p>
 * <p>
 * Bean wiring is defined (but may be commented out) in {@code IdRepoConfig}
 * within {@code id-repository-service}.
 * </p>
 *
 * @author Manoj SP
 * @see ShardResolver
 */
public class ShardDataSourceResolver extends AbstractRoutingDataSource {

	/** Thread-local holder for the active shard lookup key. */
	private static final ThreadLocal<Object> currentShard = new ThreadLocal<>();

	/**
	 * Returns the shard key for the current thread, used by the routing datasource
	 * to select the target {@code DataSource}.
	 *
	 * @return current shard name, or {@code null} if not set
	 */
	@Override
	protected Object determineCurrentLookupKey() {
		return ShardDataSourceResolver.getCurrentShard();
	}

	/**
	 * Sets the shard name for the current thread before a sharded DB operation.
	 *
	 * @param shard shard lookup key returned by {@link ShardResolver#getShard(String)}
	 */
	public static void setCurrentShard(String shard) {
		currentShard.set(shard);
	}

	/**
	 * Returns the shard name previously set on the current thread.
	 *
	 * @return current shard lookup key, or {@code null} if unset
	 */
	public static Object getCurrentShard() {
		return currentShard.get();
	}

	/**
	 * Clears the thread-local shard key. Must be called after sharded operations
	 * complete to prevent incorrect routing on subsequent requests.
	 */
	public static void resetShardConfig() {
		currentShard.remove();
	}
}