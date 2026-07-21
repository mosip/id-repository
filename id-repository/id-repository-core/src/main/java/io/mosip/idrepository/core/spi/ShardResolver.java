package io.mosip.idrepository.core.spi;

import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * SPI for resolving a database shard name from an individual identifier.
 * <p>
 * In multi-shard deployments, the shard determines which physical database
 * holds the UIN record. The resolved name is placed on the current thread via
 * {@link ShardDataSourceResolver#setCurrentShard(String)} before repository
 * access.
 * </p>
 * <p>
 * <b>Implementor:</b> {@code DefaultShardResolver} in {@code id-repository-service}.
 * </p>
 * <p>
 * <b>Callers:</b> identity service layer before read/write operations on
 * sharded datasources (wired from {@code IdRepoConfig} when enabled).
 * </p>
 *
 * @author Manoj SP
 * @see ShardDataSourceResolver
 */
public interface ShardResolver {

	/**
	 * Resolves the shard name for the given identifier.
	 *
	 * @param id individual identifier (typically UIN or hash)
	 * @return shard lookup key used by {@link ShardDataSourceResolver}
	 * @throws IdRepoAppException if the identifier cannot be mapped to a shard
	 */
	String getShard(String id) throws IdRepoAppException;
}