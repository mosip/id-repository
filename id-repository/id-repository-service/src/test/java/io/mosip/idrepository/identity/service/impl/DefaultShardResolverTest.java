package io.mosip.idrepository.identity.service.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * Unit tests for {@link DefaultShardResolver}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultShardResolverTest {

	@InjectMocks
	private DefaultShardResolver shardResolver;

	@Test
	public void getShardReturnsShard1WhenIdStartsWithZeroToFour() throws IdRepoAppException {
		assertEquals("shard1", shardResolver.getShard("0abc"));
		assertEquals("shard1", shardResolver.getShard("4xyz"));
	}

	@Test
	public void getShardReturnsShard2Otherwise() throws IdRepoAppException {
		assertEquals("shard2", shardResolver.getShard("5abc"));
		assertEquals("shard2", shardResolver.getShard("9xyz"));
		assertEquals("shard2", shardResolver.getShard("abc"));
	}
}
