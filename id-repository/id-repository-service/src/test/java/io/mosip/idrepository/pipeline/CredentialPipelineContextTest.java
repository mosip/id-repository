package io.mosip.idrepository.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Constructor;

import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link CredentialPipelineContext}.
 */
public class CredentialPipelineContextTest {

	private static final String PLAIN_UIN = "1234567890123456";
	private static final String ENCRYPTED_UIN = "encrypted-uin";
	private static final String TRIGGER_ACTION = "CREATE";

	@After
	public void tearDown() {
		CredentialPipelineContext.clear();
	}

	@Test
	public void setGetAndClear() {
		assertNull(CredentialPipelineContext.get());

		CredentialPipelineContext.set(PLAIN_UIN, ENCRYPTED_UIN, TRIGGER_ACTION);
		CredentialPipelineContext.State state = CredentialPipelineContext.get();

		assertNotNull(state);
		assertEquals(PLAIN_UIN, state.getPlainIndividualId());
		assertEquals(ENCRYPTED_UIN, state.getEncryptedIndividualId());
		assertEquals(TRIGGER_ACTION, state.getTriggerAction());

		CredentialPipelineContext.clear();
		assertNull(CredentialPipelineContext.get());
	}

	@Test
	public void stateConstructorAndGetters() {
		CredentialPipelineContext.State state = new CredentialPipelineContext.State(PLAIN_UIN, ENCRYPTED_UIN,
				TRIGGER_ACTION);

		assertEquals(PLAIN_UIN, state.getPlainIndividualId());
		assertEquals(ENCRYPTED_UIN, state.getEncryptedIndividualId());
		assertEquals(TRIGGER_ACTION, state.getTriggerAction());
		assertNotNull(state.getIdentityCache());
	}

	@Test
	public void attachSharesSameStateInstance() {
		CredentialPipelineContext.State state = new CredentialPipelineContext.State(PLAIN_UIN, ENCRYPTED_UIN,
				TRIGGER_ACTION);
		CredentialPipelineContext.attach(state);
		assertEquals(state, CredentialPipelineContext.get());
		assertEquals(state.getIdentityCache(), CredentialPipelineContext.get().getIdentityCache());
	}

	@Test
	public void privateConstructor() throws Exception {
		Constructor<CredentialPipelineContext> constructor = CredentialPipelineContext.class
				.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();
	}
}
