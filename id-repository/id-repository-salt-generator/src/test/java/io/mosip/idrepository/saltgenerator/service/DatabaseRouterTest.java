package io.mosip.idrepository.saltgenerator.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.zaxxer.hikari.HikariDataSource;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.util.EnvUtil;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseRouterTest {

	@Mock
	private EnvUtil env;

	private DatabaseRouter router;
	private HikariDataSource primary;
	private HikariDataSource secondary;

	@Before
	public void setUp() {
		when(env.getProperty(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
		when(env.getProperty(IdRepoConstants.IDENTITY_DB_URL))
				.thenReturn("jdbc:h2:mem:saltgen_idrepo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
		when(env.getProperty(IdRepoConstants.IDENTITY_DB_USERNAME)).thenReturn("sa");
		when(env.getProperty(IdRepoConstants.IDENTITY_DB_PASSWORD)).thenReturn("");
		when(env.getProperty(IdRepoConstants.IDENTITY_DB_DRIVER_CLASS_NAME)).thenReturn("org.h2.Driver");
		when(env.getProperty(IdRepoConstants.VID_DB_URL))
				.thenReturn("jdbc:h2:mem:saltgen_idmap;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
		when(env.getProperty(IdRepoConstants.VID_DB_USERNAME)).thenReturn("sa");
		when(env.getProperty(IdRepoConstants.VID_DB_PASSWORD)).thenReturn("");
		when(env.getProperty(IdRepoConstants.VID_DB_DRIVER_CLASS_NAME)).thenReturn("org.h2.Driver");
		router = new DatabaseRouter(env);
	}

	@After
	public void tearDown() {
		if (primary != null) {
			primary.close();
		}
		if (secondary != null) {
			secondary.close();
		}
	}

	@Test
	public void primaryDataSourceUsesSaltgenIdrepoPoolSettings() {
		primary = (HikariDataSource) router.primaryDataSource();

		assertEquals("saltgen-idrepo-pool", primary.getPoolName());
		assertEquals(2, primary.getMaximumPoolSize());
		assertEquals(1, primary.getMinimumIdle());
		assertTrue(primary.getJdbcUrl().contains("saltgen_idrepo"));
	}

	@Test
	public void secondaryDataSourceUsesSaltgenIdmapPoolSettings() {
		secondary = (HikariDataSource) router.secondaryDataSource();

		assertEquals("saltgen-idmap-pool", secondary.getPoolName());
		assertEquals(2, secondary.getMaximumPoolSize());
		assertEquals(1, secondary.getMinimumIdle());
		assertTrue(secondary.getJdbcUrl().contains("saltgen_idmap"));
	}
}
