package io.mosip.idrepository.saltgenerator;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.idrepository.saltgenerator.service.SaltGenerator;

/**
 * Unit tests for {@link SaltGeneratorRunner}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SaltGeneratorRunnerTest {

	@Mock
	private SaltGenerator saltGenerator;

	@InjectMocks
	private SaltGeneratorRunner runner;

	@Test
	public void runInvokesSaltGeneratorStart() throws Exception {
		runner.run();

		verify(saltGenerator).start();
	}

	@Test
	public void runPropagatesSaltGeneratorFailures() throws Exception {
		doThrow(new SQLException("db down")).when(saltGenerator).start();

		assertThrows(SQLException.class, () -> runner.run());
		verify(saltGenerator).start();
	}
}
