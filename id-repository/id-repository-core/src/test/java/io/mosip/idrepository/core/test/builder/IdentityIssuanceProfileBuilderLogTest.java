package io.mosip.idrepository.core.test.builder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;

/**
 * Failure paths of {@link IdentityIssuanceProfileBuilder} must not write identity values or
 * exception text (which quotes them) to the log (CWE-532).
 */
public class IdentityIssuanceProfileBuilderLogTest {

	private final ObjectMapper mapper = new ObjectMapper();

	private String identityData;

	private Logger builderLogger;

	private ListAppender<ILoggingEvent> appender;

	@Before
	public void init() throws IOException {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		IdentityIssuanceProfileBuilder.setIdentityMapping(mapper.readValue(
				IOUtils.toString(getClass().getClassLoader().getResourceAsStream("identity-mapping.json"),
						StandardCharsets.UTF_8),
				IdentityMapping.class));
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu/MM/dd");
		identityData = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		builderLogger = (Logger) LoggerFactory.getLogger(IdentityIssuanceProfileBuilder.class);
		appender = new ListAppender<>();
		appender.start();
		builderLogger.addAppender(appender);
	}

	@After
	public void tearDown() {
		builderLogger.detachAppender(appender);
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu/MM/dd");
	}

	@Test
	public void dobFormatMismatchDoesNotLogDob() {
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu-MM-dd");
		build(identityData.getBytes(StandardCharsets.UTF_8));
		assertNoLeak("1972/05/07", "1972\\/05\\/07");
	}

	@Test
	public void malformedIdentityJsonDoesNotLogDocument() {
		build("{\"dateOfBirth\":\"1987/06/05\",\"fullName\":\"Synthetic Testperson\", BROKEN"
				.getBytes(StandardCharsets.UTF_8));
		assertNoLeak("Synthetic Testperson", "1987/06/05", "BROKEN");
	}

	@Test
	public void wrongShapeAttributeDoesNotLogValue() throws IOException {
		ObjectNode identity = (ObjectNode) mapper.readTree(identityData);
		identity.putArray("gender").add("SecretGenderValue");
		identity.putArray("region").add("SecretRegionValue");
		build(mapper.writeValueAsBytes(identity));
		assertNoLeak("SecretGenderValue", "SecretRegionValue");
	}

	private IdentityIssuanceProfile build(byte[] newIdentity) {
		return IdentityIssuanceProfile.builder().setProcessName("New").setNewIdentity(newIdentity).build();
	}

	private void assertNoLeak(String... values) {
		assertFalse("expected the failure path to log a warning", appender.list.isEmpty());
		for (ILoggingEvent event : appender.list) {
			assertNull("exception must not be attached to the log event", event.getThrowableProxy());
			String message = event.getFormattedMessage();
			for (String value : values) {
				assertTrue("log leaked '" + value + "': " + message, !message.contains(value));
			}
		}
	}
}
