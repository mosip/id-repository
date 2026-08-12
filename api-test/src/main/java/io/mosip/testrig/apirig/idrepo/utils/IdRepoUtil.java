package io.mosip.testrig.apirig.idrepo.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.SkipException;

import io.mosip.testrig.apirig.dbaccess.DBManager;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;

public class IdRepoUtil extends AdminTestUtil {

	private static final Logger logger = Logger.getLogger(IdRepoUtil.class);
	public static String genRidExt = "23456" + generateRandomNumberString(10);
	public static List<String> testCasesInRunScope = new ArrayList<>();
	
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}
	
	public static String isTestCaseValidForExecution(TestCaseDTO testCaseDTO) {
		String testCaseName = testCaseDTO.getTestCaseName();
		currentTestCaseName = testCaseName;

		int indexof = testCaseName.indexOf("_");
		String modifiedTestCaseName = testCaseName.substring(indexof + 1);

		addTestCaseDetailsToMap(modifiedTestCaseName, testCaseDTO.getUniqueIdentifier());
		
		if (!testCasesInRunScope.isEmpty()
				&& testCasesInRunScope.contains(testCaseDTO.getUniqueIdentifier()) == false) {
			throw new SkipException(GlobalConstants.NOT_IN_RUN_SCOPE_MESSAGE);
		}
		
		// Handle extra workflow dependencies
		if (testCaseDTO != null && testCaseDTO.getAdditionalDependencies() != null
				&& AdminTestUtil.generateDependency == true) {
			addAdditionalDependencies(testCaseDTO);
		}

		if (SkipTestCaseHandler.isTestCaseInSkippedList(testCaseName)) {
			throw new SkipException(GlobalConstants.KNOWN_ISSUES);
		}

		// The schema-conditional skips below use FEATURE_NOT_SUPPORTED_MESSAGE so EmailableReport routes
		// them to the Ignored bucket (it matches on that phrase), not Skipped.
		if (testCaseDTO.getRequiredSchemaFields() != null && testCaseDTO.getRequiredSchemaFields().length > 0) {
			for (String field : testCaseDTO.getRequiredSchemaFields()) {
				String trimmed = field.trim();
				if (globalRequiredFields == null || !isElementPresent(globalRequiredFields, trimmed)) {
					throw new SkipException("Schema field '" + trimmed + "' not present in current IdSchema — "
							+ GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
				}
			}
		}

		// Skip-only handle gate: skip a test whose listed field isn't a handle in the live schema.
		if (testCaseDTO.getRequiredHandleFields() != null && testCaseDTO.getRequiredHandleFields().length > 0) {
			JSONObject identityProps = AdminTestUtil.getIdentitySchemaProperties();
			for (String field : testCaseDTO.getRequiredHandleFields()) {
				String trimmed = field.trim();
				if (!isFieldHandleCapable(identityProps, trimmed)) {
					throw new SkipException("Schema field '" + trimmed + "' is not a handle in the current IdSchema — "
							+ GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
				}
			}
		}

		// Schema-capability skips for the generic (field-name-agnostic) handle scenarios. Each test
		// declares the capability it needs via its name; if the live schema can't support it, skip.
		if (testCaseName.contains("_missingRequiredHandle") && !schemaHasRequiredHandle()) {
			throw new SkipException(
					"No required handle field in the current IdSchema — " + GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}
		if (testCaseName.contains("_invalidStringHandleValue") && !schemaHasHandleOfType("string")) {
			throw new SkipException(
					"No string-typed handle in the current IdSchema — " + GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}
		if (testCaseName.contains("_invalidArrayHandleValue") && !schemaHasHandleOfType("array")) {
			throw new SkipException(
					"No array-typed handle in the current IdSchema — " + GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}
		if (testCaseName.contains("_appendUntaggedValuesToArrayHandle") && !schemaHasHandleOfType("array")) {
			throw new SkipException(
					"No array-typed handle in the current IdSchema — " + GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}
		if (testCaseName.contains("_extraNonSchemaField") && schemaAllowsAdditionalProperties()) {
			throw new SkipException("Schema permits additional properties, unknown-field rejection not applicable — "
					+ GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}
		if (testCaseName.contains("_optionalFieldNotInAdd") && !schemaHasOptionalClaimableField()) {
			throw new SkipException("No optional (non-required) claimable field in the current IdSchema — "
					+ GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		JSONArray dobArray = null;
		JSONArray emailArray = null;
		String dobActuator = getValueFromAuthActuator("json-property", "dob");
		String emailActuator = getValueFromAuthActuator("json-property", "emailId");
		if (dobActuator != null && !dobActuator.isBlank()) {
			dobArray = new JSONArray(dobActuator);
		}
		if (emailActuator != null && !emailActuator.isBlank()) {
			emailArray = new JSONArray(emailActuator);
		}
		String dob = (dobArray != null && !dobArray.isEmpty()) ? dobArray.getString(0) : "dateOfBirth";
		String email = (emailArray != null && !emailArray.isEmpty()) ? emailArray.getString(0) : "email";

		if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("DOB")
				&& (!isElementPresent(globalRequiredFields, dob))) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("_handle")
				&& !schemaHasAnyHandle()) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}


		if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("Email")
				&& (!isElementPresent(globalRequiredFields, email))) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		else if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("Invalid_BioVal")
				&& (ConfigManager.isInServiceNotDeployedList(GlobalConstants.ADMIN))) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		return testCaseName;
	}

	/** True when the named field exists in the schema and is declared "handle": true (case-insensitive). */
	private static boolean isFieldHandleCapable(JSONObject identityProps, String fieldName) {
		for (String key : identityProps.keySet()) {
			if (key.equalsIgnoreCase(fieldName)) {
				return identityProps.getJSONObject(key).optBoolean("handle", false);
			}
		}
		return false;
	}

	public static void dbCleanUp() {
		runCleanup("keymgr", () -> DBManager.executeDBQueries(IdRepoConfigManager.getKMDbUrl(),
				IdRepoConfigManager.getKMDbUser(), IdRepoConfigManager.getKMDbPass(),
				IdRepoConfigManager.getKMDbSchema(),
				getGlobalResourcePath() + "/" + "config/keyManagerCertDataDeleteQueries.txt"));
		runCleanup("ida", () -> DBManager.executeDBQueries(IdRepoConfigManager.getIdaDbUrl(),
				IdRepoConfigManager.getIdaDbUser(), IdRepoConfigManager.getPMSDbPass(),
				IdRepoConfigManager.getIdaDbSchema(),
				getGlobalResourcePath() + "/" + "config/idaCertDataDeleteQueries.txt"));
		runCleanup("master", () -> DBManager.executeDBQueries(IdRepoConfigManager.getMASTERDbUrl(),
				IdRepoConfigManager.getMasterDbUser(), IdRepoConfigManager.getMasterDbPass(),
				IdRepoConfigManager.getMasterDbSchema(),
				getGlobalResourcePath() + "/" + "config/masterDataCertDataDeleteQueries.txt"));
		runCleanup("idrepo", () -> DBManager.executeDBQueries(IdRepoConfigManager.getIdRepoDbUrl(),
				IdRepoConfigManager.getIdRepoDbUser(), IdRepoConfigManager.getPMSDbPass(), "idrepo",
				getGlobalResourcePath() + "/" + "config/idrepoDeleteQueries.txt"));
	}

	private static void runCleanup(String schema, Runnable cleanup) {
		try {
			cleanup.run();
		} catch (Throwable t) {
			logger.error("DB cleanup skipped for " + schema + ": " + t.getClass().getSimpleName() + " - "
					+ t.getMessage());
		}
	}
	
	public static String inputStringKeyWordHandeler(String jsonString, String testCaseName) {
		if (jsonString == null) {
			logger.info(" Request Json String is :" + jsonString);
			return jsonString;
		}

		if (jsonString.contains("$RIDEXT$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDEXT$", genRidExt);
		return jsonString;
	}

	/** Resolves $HANDLEVALUE:&lt;field&gt;$ tokens; delegates to {@link AdminTestUtil}. */
	public static String resolveGenericHandleValueTokens(String jsonString) {
		return AdminTestUtil.resolveSchemaHandleValueTokens(jsonString);
	}

	/** Generates a value satisfying the given schema field's validator; delegates to {@link AdminTestUtil}. */
	public static String generateSchemaFieldValue(String fieldName) {
		return AdminTestUtil.generateSchemaFieldValue(fieldName);
	}

	/** True when the live IdSchema declares at least one handle field. */
	public static boolean schemaHasAnyHandle() {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		for (String fieldName : props.keySet()) {
			if (props.getJSONObject(fieldName).optBoolean("handle", false)) {
				return true;
			}
		}
		return false;
	}

	/** True when the schema declares at least one handle field of the given "type" ("string"/"array"). */
	public static boolean schemaHasHandleOfType(String type) {
		return resolveHandleOfType(type) != null;
	}

	/** First handle field of the given schema "type", or null if none — schema-driven, no field names. */
	public static String resolveHandleOfType(String type) {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		List<String> fieldNames = new ArrayList<>(props.keySet());
		Collections.sort(fieldNames);
		for (String fieldName : fieldNames) {
			JSONObject fieldDef = props.getJSONObject(fieldName);
			if (fieldDef.optBoolean("handle", false) && type.equals(fieldDef.optString("type", "string"))) {
				return fieldName;
			}
		}
		return null;
	}

	/** True when the schema declares a handle field that is also in its "required" array. */
	public static boolean schemaHasRequiredHandle() {
		return resolveRequiredHandle() != null;
	}

	/** First handle field that is also required, or null if none. */
	public static String resolveRequiredHandle() {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		List<String> fieldNames = new ArrayList<>(props.keySet());
		Collections.sort(fieldNames);
		for (String fieldName : fieldNames) {
			if (props.getJSONObject(fieldName).optBoolean("handle", false)
					&& isElementPresent(globalRequiredFields, fieldName)) {
				return fieldName;
			}
		}
		return null;
	}

	/** True when the schema's identity node permits fields it does not define ("additionalProperties":true). */
	public static boolean schemaAllowsAdditionalProperties() {
		AdminTestUtil.getIdentitySchemaProperties(); // ensure the flag is populated
		return Boolean.TRUE.equals(AdminTestUtil.globalIdentityAdditionalProperties);
	}

	/** First schema field that's claimable but not required — resolved from the live schema, excluding handles and complex $ref types. */
	public static String resolveOptionalClaimableField() {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		List<String> fieldNames = new ArrayList<>(props.keySet());
		Collections.sort(fieldNames);
		for (String fieldName : fieldNames) {
			if (isElementPresent(globalRequiredFields, fieldName)) {
				continue;
			}
			JSONObject fieldDef = props.getJSONObject(fieldName);
			if (fieldDef.optBoolean("handle", false)) {
				continue;
			}
			String ref = fieldDef.optString("$ref", "");
			if (ref.contains("documentType") || ref.contains("biometricsType") || ref.contains("hashType")
					|| ref.contains("TaggedListType")) {
				continue;
			}
			if (fieldName.equals("selectedHandles") || fieldName.equals("IDSchemaVersion")) {
				continue;
			}
			return fieldName;
		}
		return null;
	}

	/** True when the schema has at least one field {@link #resolveOptionalClaimableField()} would return. */
	public static boolean schemaHasOptionalClaimableField() {
		return resolveOptionalClaimableField() != null;
	}

}