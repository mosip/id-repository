package io.mosip.idrepository.saltgenerator.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SaltJdbcWriterTest {

	private DataSource idRepoDataSource;
	private DataSource idMapDataSource;
	private Connection idRepoConnection;
	private Connection idMapConnection;
	private SaltJdbcWriter writer;

	@Before
	public void setUp() throws SQLException {
		idRepoDataSource = mock(DataSource.class);
		idMapDataSource = mock(DataSource.class);
		idRepoConnection = mock(Connection.class);
		idMapConnection = mock(Connection.class);
		when(idRepoDataSource.getConnection()).thenReturn(idRepoConnection);
		when(idMapDataSource.getConnection()).thenReturn(idMapConnection);
		writer = new SaltJdbcWriter(idRepoDataSource, idMapDataSource);
	}

	@Test
	public void resolveResumeStartUsesConfiguredStartWhenTablesEmpty() throws SQLException {
		mockMaxId(idRepoDataSource, -1L);
		mockMaxId(idMapDataSource, -1L);

		assertEquals(1L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void resolveResumeStartResumesFromMinMaxPlusOne() throws SQLException {
		mockMaxId(idRepoDataSource, 5L);
		mockMaxId(idMapDataSource, 7L);

		assertEquals(6L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void resolveResumeStartReturnsBeyondEndWhenAlreadyPopulated() throws SQLException {
		mockMaxId(idRepoDataSource, 10L);
		mockMaxId(idMapDataSource, 10L);

		assertEquals(11L, writer.resolveResumeStart(1L, 10L));
	}

	@Test
	public void resolveResumeStartKeepsConfiguredStartWhenHigherThanMaxPlusOne() throws SQLException {
		mockMaxId(idRepoDataSource, 3L);
		mockMaxId(idMapDataSource, 3L);

		assertEquals(10L, writer.resolveResumeStart(10L, 100L));
	}

	@Test
	public void writeChunkInsertsMultipleRowsInOneBatch() throws SQLException {
		PreparedStatement idRepoHash = mock(PreparedStatement.class);
		PreparedStatement idRepoEncrypt = mock(PreparedStatement.class);
		PreparedStatement idMapHash = mock(PreparedStatement.class);
		PreparedStatement idMapEncrypt = mock(PreparedStatement.class);
		when(idRepoConnection.prepareStatement(anyString())).thenReturn(idRepoHash, idRepoEncrypt);
		when(idMapConnection.prepareStatement(anyString())).thenReturn(idMapHash, idMapEncrypt);

		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		writer.writeChunk(List.of(
				new SaltRow(1L, "h1", "i1", "v1", now),
				new SaltRow(2L, "h2", "i2", "v2", now)));

		verify(idRepoHash, times(2)).addBatch();
		verify(idRepoEncrypt, times(2)).addBatch();
		verify(idMapHash, times(2)).addBatch();
		verify(idMapEncrypt, times(2)).addBatch();
		verify(idRepoConnection).commit();
		verify(idMapConnection).commit();
	}

	@Test
	public void writeChunkInsertsIntoAllFourTables() throws SQLException {
		PreparedStatement idRepoHash = mock(PreparedStatement.class);
		PreparedStatement idRepoEncrypt = mock(PreparedStatement.class);
		PreparedStatement idMapHash = mock(PreparedStatement.class);
		PreparedStatement idMapEncrypt = mock(PreparedStatement.class);
		when(idRepoConnection.prepareStatement(anyString())).thenReturn(idRepoHash, idRepoEncrypt);
		when(idMapConnection.prepareStatement(anyString())).thenReturn(idMapHash, idMapEncrypt);

		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		writer.writeChunk(List.of(new SaltRow(1L, "hash", "idEnc", "vidEnc", now)));

		verify(idRepoConnection).commit();
		verify(idMapConnection).commit();
		verify(idRepoHash).executeBatch();
		verify(idRepoEncrypt).executeBatch();
		verify(idMapHash).executeBatch();
		verify(idMapEncrypt).executeBatch();
	}

	@Test
	public void writeChunkIgnoresNullOrEmptyList() throws SQLException {
		writer.writeChunk(null);
		writer.writeChunk(List.of());

		verify(idRepoDataSource, never()).getConnection();
		verify(idMapDataSource, never()).getConnection();
	}

	@Test
	public void writeChunkRollsBackOnFailure() throws SQLException {
		PreparedStatement failing = mock(PreparedStatement.class);
		when(idRepoConnection.prepareStatement(anyString())).thenReturn(failing);
		when(failing.executeBatch()).thenThrow(new SQLException("batch failed"));

		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		try {
			writer.writeChunk(List.of(new SaltRow(1L, "hash", "idEnc", "vidEnc", now)));
		} catch (SQLException ignored) {
		}

		verify(idRepoConnection).rollback();
		verify(idMapDataSource, never()).getConnection();
	}

	@Test
	public void queryMaxIdReturnsNegativeOneOnSqlError() throws SQLException {
		when(idRepoDataSource.getConnection()).thenThrow(new SQLException("broken"));
		when(idMapDataSource.getConnection()).thenThrow(new SQLException("broken"));

		assertEquals(1L, writer.resolveResumeStart(1L, 10L));
	}

	@Test
	public void resolveResumeStartUsesIdMapWhenIdRepoEmpty() throws SQLException {
		mockMaxId(idRepoDataSource, -1L);
		mockMaxId(idMapDataSource, 4L);

		assertEquals(5L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void resolveResumeStartUsesIdRepoWhenIdMapEmpty() throws SQLException {
		mockMaxId(idRepoDataSource, 8L);
		mockMaxId(idMapDataSource, -1L);

		assertEquals(9L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void resolveResumeStartKeepsConfiguredStartWhenMaxIsLongMax() throws SQLException {
		mockMaxId(idRepoDataSource, Long.MAX_VALUE);
		mockMaxId(idMapDataSource, Long.MAX_VALUE);

		assertEquals(1L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void queryMaxIdReturnsNegativeOneWhenResultSetEmpty() throws SQLException {
		mockEmptyMaxId(idRepoDataSource);
		mockEmptyMaxId(idMapDataSource);

		assertEquals(1L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void queryMaxIdReturnsNegativeOneWhenCloseFails() throws SQLException {
		mockMaxIdWithCloseFailure(idRepoDataSource);
		mockMaxIdWithCloseFailure(idMapDataSource);

		assertEquals(1L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void queryMaxIdReturnsNegativeOneWhenPrepareFails() throws SQLException {
		mockPrepareFailure(idRepoDataSource);
		mockPrepareFailure(idMapDataSource);

		assertEquals(1L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void queryMaxIdReturnsNegativeOneWhenExecuteFails() throws SQLException {
		mockExecuteFailure(idRepoDataSource);
		mockExecuteFailure(idMapDataSource);

		assertEquals(1L, writer.resolveResumeStart(1L, 100L));
	}

	@Test
	public void writeChunkRollsBackIdMapOnFailure() throws SQLException {
		PreparedStatement idRepoHash = mock(PreparedStatement.class);
		PreparedStatement idRepoEncrypt = mock(PreparedStatement.class);
		PreparedStatement failing = mock(PreparedStatement.class);
		when(idRepoConnection.prepareStatement(anyString())).thenReturn(idRepoHash, idRepoEncrypt);
		when(idMapConnection.prepareStatement(anyString())).thenReturn(failing);
		when(failing.executeBatch()).thenThrow(new SQLException("idmap batch failed"));

		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		try {
			writer.writeChunk(List.of(new SaltRow(1L, "hash", "idEnc", "vidEnc", now)));
		} catch (SQLException ignored) {
		}

		verify(idRepoConnection).commit();
		verify(idMapConnection).rollback();
	}

	private void mockMaxId(DataSource dataSource, long maxId) throws SQLException {
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getLong(1)).thenReturn(maxId);
	}

	private void mockEmptyMaxId(DataSource dataSource) throws SQLException {
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);
	}

	private void mockMaxIdWithCloseFailure(DataSource dataSource) throws SQLException {
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getLong(1)).thenReturn(5L);
		doThrow(new SQLException("close failed")).when(resultSet).close();
	}

	private void mockPrepareFailure(DataSource dataSource) throws SQLException {
		Connection connection = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenThrow(new SQLException("prepare failed"));
	}

	private void mockExecuteFailure(DataSource dataSource) throws SQLException {
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenThrow(new SQLException("execute failed"));
	}
}
