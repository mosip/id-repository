package io.mosip.idrepository.core.test.validator;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.mosip.idrepository.core.validator.IdRepoValidationMessageHelper;

public class IdRepoValidationMessageHelperTest {

	@Test
	public void formatAllowedListSortsAndQuotesValues() {
		assertEquals("[\"mosip.vid.deactivate\", \"mosip.vid.reactivate\"]",
				IdRepoValidationMessageHelper.formatAllowedList(
						List.of("mosip.vid.reactivate", "mosip.vid.deactivate")));
	}

	@Test
	public void missingIdIncludesExpectedOperationValue() {
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("deactivate", "mosip.vid.deactivate");
		ids.put("reactivate", "mosip.vid.reactivate");

		String message = IdRepoValidationMessageHelper.missingId("deactivate", ids);

		assertEquals(
				"id - missing; expected \"mosip.vid.deactivate\" for \"deactivate\" operation; allowed values: [\"mosip.vid.deactivate\", \"mosip.vid.reactivate\"]",
				message);
	}

	@Test
	public void invalidIdIncludesReceivedAndExpectedValues() {
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("deactivate", "mosip.vid.deactivate");
		ids.put("reactivate", "mosip.vid.reactivate");

		String message = IdRepoValidationMessageHelper.invalidId("wrong-id", "deactivate", ids);

		assertEquals(
				"id - received \"wrong-id\"; expected \"mosip.vid.deactivate\" for \"deactivate\" operation; allowed values: [\"mosip.vid.deactivate\", \"mosip.vid.reactivate\"]",
				message);
	}

	@Test
	public void formatAllowedListReturnsEmptyBracketsForNullOrEmpty() {
		assertEquals("[]", IdRepoValidationMessageHelper.formatAllowedList(null));
		assertEquals("[]", IdRepoValidationMessageHelper.formatAllowedList(List.of()));
	}

	@Test
	public void missingIdWithoutExpectedOperationUsesAllowedValuesOnly() {
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("deactivate", "mosip.vid.deactivate");
		assertEquals("id - missing; allowed values: [\"mosip.vid.deactivate\"]",
				IdRepoValidationMessageHelper.missingId("unknown", ids));
	}

	@Test
	public void invalidIdWithoutExpectedOperationUsesAllowedValuesOnly() {
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("deactivate", "mosip.vid.deactivate");
		assertEquals("id - received \"wrong\"; allowed values: [\"mosip.vid.deactivate\"]",
				IdRepoValidationMessageHelper.invalidId("wrong", "unknown", ids));
	}

	@Test
	public void missingVersionWithAndWithoutExpected() {
		assertEquals("version - missing; expected \"1.0\"; must match pattern ^\\d+\\.\\d+$",
				IdRepoValidationMessageHelper.missingVersion("^\\d+\\.\\d+$", "1.0"));
		assertEquals("version - missing; must match pattern ^\\d+\\.\\d+$",
				IdRepoValidationMessageHelper.missingVersion("^\\d+\\.\\d+$", " "));
	}

	@Test
	public void invalidVersionWithAndWithoutExpected() {
		assertEquals("version - received \"bad\"; expected \"1.0\"; must match pattern ^\\d+\\.\\d+$",
				IdRepoValidationMessageHelper.invalidVersion("bad", "^\\d+\\.\\d+$", "1.0"));
		assertEquals("version - received \"bad\"; must match pattern ^\\d+\\.\\d+$",
				IdRepoValidationMessageHelper.invalidVersion("bad", "^\\d+\\.\\d+$", null));
	}

	@Test
	public void missingFieldAndRequestTime() {
		assertEquals("status - missing", IdRepoValidationMessageHelper.missingField("status"));
		assertEquals(
				"requesttime - missing; provide the current UTC timestamp in ISO-8601 format (e.g. \"2026-07-06T14:07:54.716Z\")",
				IdRepoValidationMessageHelper.missingRequestTime());
	}

	@Test
	public void invalidRequestTimeDeviationWithoutTimestamp() {
		assertEquals("requesttime - must be within ±30 seconds of the current UTC time",
				IdRepoValidationMessageHelper.invalidRequestTimeDeviation(30));
	}

	@Test
	public void missingAndInvalidWithAllowedValues() {
		assertEquals("status - missing; allowed values: [\"ACTIVE\", \"INACTIVE\"]",
				IdRepoValidationMessageHelper.missingWithAllowed("status", List.of("ACTIVE", "INACTIVE")));
		assertEquals("status - received \"BAD\"; allowed values: [\"ACTIVE\", \"INACTIVE\"]",
				IdRepoValidationMessageHelper.invalidWithAllowed("status", "BAD", List.of("ACTIVE", "INACTIVE")));
	}

	@Test
	public void invalidUinAndRegisteredStatusMessages() {
		assertEquals("UIN - must be a valid 10-digit UIN (checksum validation failed)",
				IdRepoValidationMessageHelper.invalidUin());
		assertEquals("status \"PENDING\"; expected registered UIN status \"ACTIVATED\"",
				IdRepoValidationMessageHelper.invalidRegisteredUinStatus("PENDING", "ACTIVATED"));
	}

	@Test
	public void missingIdWithNullOperationIdsUsesEmptyAllowedList() {
		assertEquals("id - missing; allowed values: []",
				IdRepoValidationMessageHelper.missingId("deactivate", null));
	}

	@Test
	public void invalidIdWithNullOperationIdsUsesEmptyAllowedList() {
		assertEquals("id - received \"wrong\"; allowed values: []",
				IdRepoValidationMessageHelper.invalidId("wrong", "deactivate", null));
	}

	@Test
	public void invalidRequestTimeDeviationIncludesReceivedTimestamp() {
		assertEquals(
				"requesttime - received \"2026-07-06T14:07:54.716Z\"; must be within ±60 seconds of the current UTC time",
				IdRepoValidationMessageHelper.invalidRequestTimeDeviation("2026-07-06T14:07:54.716Z", 60));
	}

}
