package io.mosip.idrepository.identity.service.impl;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.spi.ShardResolver;

/**
 * Resolves salt-shard / DB routing key from UIN hash for multi-DB deployments.
 */
@Component
public class DefaultShardResolver implements ShardResolver {

	private static final String SHARD1 = "shard1";

	private static final String SHARD2 = "shard2";

	private static final Pattern PATTERN = Pattern.compile("[0-4].*");

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.kernel.core.idrepo.shard.ShardResolver#getShrad(java.lang.String)
	 */
	@Override
	/**
	 * Get shard.
	 * @param id id
	 * @return string
	 */
	public String getShard(String id) throws IdRepoAppException {
		if (PATTERN.matcher(id).matches()) {
			return SHARD1;
		} else {
			return SHARD2;
		}
	}
}
