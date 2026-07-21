package io.mosip.idrepository.saltgenerator.service;

import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.CHUNK_SIZE;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.END_SEQ;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.START_SEQ;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SaltGeneratorTest {

	@Mock
	private Environment env;

	@Mock
	private SaltJdbcWriter saltJdbcWriter;

	@InjectMocks
	private SaltGenerator saltGenerator;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(saltGenerator, "env", env);
		ReflectionTestUtils.setField(saltGenerator, "saltJdbcWriter", saltJdbcWriter);
	}

	@Test
	public void startThrowsWhenSequenceBoundsMissing() {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(null);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(100L);

		assertThrows(IllegalStateException.class, () -> saltGenerator.start());
	}

	@Test
	public void startThrowsWhenEndSequenceMissing() {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(null);

		assertThrows(IllegalStateException.class, () -> saltGenerator.start());
	}

	@Test
	public void startNoOpWhenRangeAlreadyPopulated() throws Exception {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(10L);
		when(env.getProperty(CHUNK_SIZE.getValue(), Integer.class, 500)).thenReturn(500);
		when(saltJdbcWriter.resolveResumeStart(1L, 10L)).thenReturn(11L);

		saltGenerator.start();

		verify(saltJdbcWriter, never()).writeChunk(anyList());
	}

	@Test
	public void startWritesChunksForConfiguredRange() throws Exception {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(3L);
		when(env.getProperty(CHUNK_SIZE.getValue(), Integer.class, 500)).thenReturn(2);
		when(saltJdbcWriter.resolveResumeStart(1L, 3L)).thenReturn(1L);

		saltGenerator.start();

		verify(saltJdbcWriter, times(2)).writeChunk(anyList());
	}

	@Test
	public void startUsesDefaultChunkSizeWhenConfiguredInvalid() throws Exception {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(CHUNK_SIZE.getValue(), Integer.class, 500)).thenReturn(0);
		when(saltJdbcWriter.resolveResumeStart(1L, 1L)).thenReturn(1L);

		saltGenerator.start();

		verify(saltJdbcWriter).writeChunk(anyList());
	}

	@Test
	public void startLogsProgressAtTenThousandBoundary() throws Exception {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(10_000L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(10_000L);
		when(env.getProperty(CHUNK_SIZE.getValue(), Integer.class, 500)).thenReturn(1);
		when(saltJdbcWriter.resolveResumeStart(10_000L, 10_000L)).thenReturn(10_000L);

		saltGenerator.start();

		verify(saltJdbcWriter).writeChunk(anyList());
	}

	@Test
	public void startUsesDefaultChunkSizeWhenPropertyAbsent() throws Exception {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(CHUNK_SIZE.getValue(), Integer.class, 500)).thenReturn(500);
		when(saltJdbcWriter.resolveResumeStart(1L, 1L)).thenReturn(1L);

		saltGenerator.start();

		verify(saltJdbcWriter).writeChunk(anyList());
	}

	@Test
	public void startWritesRemainderChunkWhenRangeNotDivisibleByChunkSize() throws Exception {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(5L);
		when(env.getProperty(CHUNK_SIZE.getValue(), Integer.class, 500)).thenReturn(2);
		when(saltJdbcWriter.resolveResumeStart(1L, 5L)).thenReturn(1L);

		saltGenerator.start();

		// chunks: [1,2], [3,4], [5] → 3 writes
		verify(saltJdbcWriter, times(3)).writeChunk(anyList());
	}

	@Test
	public void startPropagatesWriterFailures() throws Exception {
		when(env.getProperty(START_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(END_SEQ.getValue(), Long.class)).thenReturn(1L);
		when(env.getProperty(CHUNK_SIZE.getValue(), Integer.class, 500)).thenReturn(500);
		when(saltJdbcWriter.resolveResumeStart(1L, 1L)).thenReturn(1L);
		doThrow(new SQLException("db down")).when(saltJdbcWriter).writeChunk(any());

		assertThrows(SQLException.class, () -> saltGenerator.start());
	}
}
