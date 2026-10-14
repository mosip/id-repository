package io.mosip.testrig.apirig.idrepo.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.SkipException;

import io.mosip.testrig.apirig.dbaccess.DBManager;
import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;

/**
 * ID Repository helpers shared by every test script in
 * {@code io.mosip.testrig.apirig.idrepo.testscripts}.
 * <p>
 * Extends the apitest-commons {@link AdminTestUtil}, so scripts inherit the REST helpers, template
 * rendering and the {@code $ID:...$} cache, and adds idrepo-specific behaviour:
 * <ul>
 * <li>{@link #isTestCaseValidForExecution(TestCaseDTO)}: one gate that decides whether a YAML case
 * runs in the current environment (run scope, known-issue list, IdSchema capabilities).</li>
 * <li>Run-scoped registration-id placeholders ({@code $RIDEXT$}, {@code $RIDV2NEW$}, ...) resolved by
 * {@link #inputStringKeyWordHandeler(String, String)}.</li>
 * <li>Schema queries (handles, required and optional fields) that keep YAML cases independent of
 * country-specific field names.</li>
 * <li>Post-run DB cleanup and request-body fix-ups such as
 * {@link #ensureDemographicDocumentsInRequest(String)}.</li>
 * </ul>
 */
public class IdRepoUtil extends AdminTestUtil {

	private static final Logger logger = Logger.getLogger(IdRepoUtil.class);

	/*
	 * Registration ids fixed once per run. Each prefix is distinct, so a draft created under one
	 * placeholder can never clash with another. A uin_draft row is unique per UIN, so one
	 * placeholder must map to one draft flow only.
	 */
	/** {@code $RIDEXT$}: v1 draft created by CreateDraft and consumed by ExtractBiometrics. */
	public static String genRidExt = "23456" + generateRandomNumberString(10);
	/** {@code $RIDDEL$}: v1 draft created by CreateDraft and discarded by DeleteDraft. */
	public static String genRidDel = "23457" + generateRandomNumberString(10);
	/** {@code $RIDV2NEW$}: main v2 draft lifecycle chain (CreateDraftV2, GetDraftV2, PublishDraftV2). */
	public static String genRidV2New = "23458" + generateRandomNumberString(10);
	/** {@code $RIDV2NEWEXPLICIT$}: v2 draft for a new identity with an explicit {@code generateUin}. */
	public static String genRidV2NewExplicit = "23459" + generateRandomNumberString(10);
	/** {@code $RIDV2UPDATEA$}: v2 draft over an existing UIN, later updated and published. */
	public static String genRidV2UpdateA = "23460" + generateRandomNumberString(10);
	/** {@code $RIDV2FRESH$}: registration id that must never have a draft (negative cases). */
	public static String genRidV2Fresh = "23461" + generateRandomNumberString(10);
	/** {@code $RIDV2LOST$}: v2 LOST-packet draft created with {@code generateUin=false}. */
	public static String genRidV2Lost = "23462" + generateRandomNumberString(10);
	/** {@code $RIDV2EXT$}: v2 draft reserved for ExtractBiometricsV2, isolated from the main chain. */
	public static String genRidV2Ext = "23463" + generateRandomNumberString(10);
	/** {@code $RIDV2UPDATEB$}: second v2 draft, over a different existing UIN than {@code $RIDV2UPDATEA$}. */
	public static String genRidV2UpdateB = "23464" + generateRandomNumberString(10);
	/**
	 * Unique identifiers of the cases allowed to run, built by the runner from
	 * {@code testCasesToExecute} plus their declared dependencies. Empty means "run everything".
	 */
	public static List<String> testCasesInRunScope = new ArrayList<>();
	
	/** Sets this class's logger to {@code ALL} in debug mode, {@code ERROR} otherwise. */
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}
	
	/**
	 * Decides whether a YAML test case runs in the current environment, and throws
	 * {@link SkipException} when it must not.
	 * <p>
	 * Checks, in order:
	 * <ol>
	 * <li>Run scope: when {@link #testCasesInRunScope} is non-empty, the case's unique identifier
	 * must be in it ({@code NOT_IN_RUN_SCOPE_MESSAGE}).</li>
	 * <li>Extra workflow dependencies are recorded when dependency generation is on.</li>
	 * <li>Known issues listed in {@code testCaseSkippedList.txt} ({@code KNOWN_ISSUES}).</li>
	 * <li>The YAML {@code requiredSchemaFields} and {@code requiredHandleFields} must exist in the
	 * live IdSchema (the latter as {@code "handle": true}).</li>
	 * <li>Name tokens that need a schema capability, for example {@code _missingRequiredHandle},
	 * {@code _invalidArrayHandleValue}, {@code _extraNonSchemaField}, {@code _optionalFieldNotInAdd},
	 * {@code _handle}, DOB / Email cases, multi-email cases that need an array-typed email, and
	 * {@code Invalid_BioVal} when the admin service is not deployed.</li>
	 * </ol>
	 * Schema-driven skips use {@code FEATURE_NOT_SUPPORTED_MESSAGE}, so the emailable report lists
	 * them as "Ignored" rather than "Skipped".
	 * <p>
	 * Side effects: sets {@code currentTestCaseName} and registers the case in the commons test case
	 * map.
	 *
	 * @param testCaseDTO the case read from YAML
	 * @return the test case name, unchanged
	 * @throws SkipException when the case must not run here
	 */
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

		// Multiple-email / tagged-email cases need email as type:array [{value, tags}].
		// requiredHandleFields only checks handle:true, so a string email handle still ran them.
		if (testRequiresArrayTypedEmail(testCaseName) && !schemaFieldIsArrayHandle(email)) {
			throw new SkipException("Schema field '" + email + "' is not an array-typed handle — "
					+ GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
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

	/** Property naming the {@code cr_by} value of rows the rig creates; see {@link #getTestDataCreator()}. */
	private static final String TEST_DATA_CREATOR_PROPERTY = "testDataCreator";

	/** Client the rig authenticates as on a deployed environment. */
	private static final String DEFAULT_TEST_DATA_CREATOR = "service-account-mosip-testrig-client";

	/** Placeholder in the idrepo, idmap and credential delete-query files. */
	private static final String TEST_DATA_CREATOR_PLACEHOLDER = "${testDataCreator}";

	/**
	 * Deletes test data from the keymanager, IDA, masterdata, idrepo, idmap (VID) and credential
	 * databases, using the SQL files under {@code config/} ({@code keyManagerCertDataDeleteQueries.txt},
	 * {@code idaCertDataDeleteQueries.txt}, {@code masterDataCertDataDeleteQueries.txt},
	 * {@code idrepoDeleteQueries.txt}, {@code idmapDeleteQueries.txt},
	 * {@code credentialDeleteQueries.txt}).
	 * <p>
	 * The idrepo, idmap and credential files delete every row whose {@code cr_by} is
	 * {@link #getTestDataCreator()}, with no age limit. Salt tables are never touched.
	 * </p>
	 * <p>
	 * Each database is cleaned independently: a failure (for example a schema that is not deployed
	 * locally) is logged and the next one is still attempted. Runs once, during suite setup,
	 * before any test case.
	 * </p>
	 */
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
		String creator = getTestDataCreator();
		logger.info("DB cleanup deletes idrepo, idmap and credential rows created by: " + creator);
		runCleanup("idrepo", () -> executeCreatorQueries(IdRepoConfigManager.getIdRepoDbUrl(), "idrepo",
				"config/idrepoDeleteQueries.txt", creator));
		runCleanup("idmap", () -> executeCreatorQueries(getDbUrl("mosip_idmap"), "idmap",
				"config/idmapDeleteQueries.txt", creator));
		runCleanup("credential", () -> executeCreatorQueries(getDbUrl("mosip_credential"), "credential",
				"config/credentialDeleteQueries.txt", creator));
	}

	/**
	 * Returns the {@code cr_by} value of rows created by the rig: property {@code testDataCreator},
	 * or {@code service-account-mosip-testrig-client} when it is not set. The service stores the
	 * caller's client as {@code cr_by}, so locally (WireMock IAM) this is
	 * {@code service-account-mosip-idrepo-client}.
	 *
	 * @return the creator to delete rows for
	 * @throws IllegalStateException if the value contains characters other than letters, digits,
	 *                               {@code .}, {@code _} and {@code -}; it is placed inside SQL
	 */
	static String getTestDataCreator() {
		String creator = null;
		try {
			creator = IdRepoConfigManager.getproperty(TEST_DATA_CREATOR_PROPERTY);
		} catch (Exception e) {
			logger.warn("Property " + TEST_DATA_CREATOR_PROPERTY + " not readable: " + e.getMessage());
		}
		if (creator == null || creator.isBlank()) {
			return DEFAULT_TEST_DATA_CREATOR;
		}
		creator = creator.trim();
		if (!creator.matches("[A-Za-z0-9._-]+")) {
			throw new IllegalStateException("Invalid " + TEST_DATA_CREATOR_PROPERTY + ": " + creator);
		}
		return creator;
	}

	/**
	 * @param database database name on the configured server, for example {@code mosip_idmap}
	 * @return JDBC URL built from {@code db-server} and {@code db-port}
	 */
	private static String getDbUrl(String database) {
		return "jdbc:postgresql://" + IdRepoConfigManager.getDbServer() + ":" + IdRepoConfigManager.getDbPort()
				+ "/" + database;
	}

	/**
	 * Runs a delete-query file after replacing {@code ${testDataCreator}} with {@code creator}.
	 * The commons runner only reads files, so the resolved queries are written to a temporary
	 * file that is removed afterwards. Uses the idrepo DB user and password.
	 *
	 * @param dbUrl        JDBC URL of the database
	 * @param schema       default schema for the session
	 * @param relativePath query file under the global resource path
	 * @param creator      value for the placeholder
	 */
	private static void executeCreatorQueries(String dbUrl, String schema, String relativePath, String creator) {
		Path resolved = null;
		try {
			String template = Files.readString(Paths.get(getGlobalResourcePath(), relativePath),
					StandardCharsets.UTF_8);
			resolved = Files.createTempFile("idrepo-cleanup-" + schema + "-", ".txt");
			Files.writeString(resolved, template.replace(TEST_DATA_CREATOR_PLACEHOLDER, creator),
					StandardCharsets.UTF_8);
			DBManager.executeDBQueries(dbUrl, IdRepoConfigManager.getIdRepoDbUser(),
					IdRepoConfigManager.getPMSDbPass(), schema, resolved.toString());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (resolved != null) {
				try {
					Files.deleteIfExists(resolved);
				} catch (IOException e) {
					logger.warn("Could not delete " + resolved + ": " + e.getMessage());
				}
			}
		}
	}

	/** Runs one cleanup step and logs, rather than propagates, any exception. */
	private static void runCleanup(String schema, Runnable cleanup) {
		try {
			cleanup.run();
		} catch (Exception e) {
			logger.error("DB cleanup skipped for " + schema + ": " + e.getClass().getSimpleName() + " - "
					+ e.getMessage());
		}
	}
	
	/**
	 * Replaces the run-scoped registration-id placeholders ({@code $RIDEXT$}, {@code $RIDDEL$},
	 * {@code $RIDV2NEW$}, {@code $RIDV2NEWEXPLICIT$}, {@code $RIDV2UPDATEA$}, {@code $RIDV2UPDATEB$},
	 * {@code $RIDV2FRESH$}, {@code $RIDV2LOST$}, {@code $RIDV2EXT$}) with the matching
	 * {@code genRid*} value. Because the values are fixed per run, create, get, update, publish and
	 * delete cases in different YAML files act on the same draft.
	 * <p>
	 * {@code $RIDV2NEWEXPLICIT$} is replaced before {@code $RIDV2NEW$} because it starts with that
	 * token.
	 *
	 * @param jsonString   request JSON, or a path-parameter string; {@code null} is returned as is
	 * @param testCaseName current test case name (not used)
	 * @return the string with every known placeholder resolved
	 */
	public static String inputStringKeyWordHandeler(String jsonString, String testCaseName) {
		if (jsonString == null) {
			logger.info(" Request Json String is :" + jsonString);
			return jsonString;
		}

		if (jsonString.contains("$RIDEXT$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDEXT$", genRidExt);
		if (jsonString.contains("$RIDDEL$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDDEL$", genRidDel);
		if (jsonString.contains("$RIDV2NEWEXPLICIT$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDV2NEWEXPLICIT$", genRidV2NewExplicit);
		if (jsonString.contains("$RIDV2NEW$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDV2NEW$", genRidV2New);
		if (jsonString.contains("$RIDV2UPDATEA$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDV2UPDATEA$", genRidV2UpdateA);
		if (jsonString.contains("$RIDV2UPDATEB$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDV2UPDATEB$", genRidV2UpdateB);
		if (jsonString.contains("$RIDV2FRESH$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDV2FRESH$", genRidV2Fresh);
		if (jsonString.contains("$RIDV2LOST$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDV2LOST$", genRidV2Lost);
		if (jsonString.contains("$RIDV2EXT$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDV2EXT$", genRidV2Ext);
		return jsonString;
	}

	/**
	 * Asserts a GetDraftV2 response has no UIN yet (LOST packet with generateUin=false).
	 * <p>
	 * Never throws: an unparsable body becomes a {@code FAIL} result.
	 *
	 * @param response GetDraftV2 response
	 * @return a "UIN" validation row: {@code PASS} when {@code response.identity.UIN} is absent,
	 *         null or blank, otherwise {@code FAIL} with the allocated UIN as actual value
	 */
	public static OutputValidationDto assertDraftUinNotAllocated(io.restassured.response.Response response) {
		OutputValidationDto dto = new OutputValidationDto();
		dto.setFieldName("UIN");
		dto.setExpValue("");
		try {
			JSONObject body = new JSONObject(response.asString());
			JSONObject resp = body.optJSONObject("response");
			JSONObject identity = resp == null ? null : resp.optJSONObject("identity");
			boolean allocated = identity != null && identity.has("UIN") && !identity.isNull("UIN")
					&& !identity.optString("UIN").isBlank();
			dto.setActualValue(allocated ? identity.optString("UIN") : "");
			dto.setStatus(allocated ? "FAIL" : "PASS");
		} catch (Exception e) {
			dto.setActualValue(e.getMessage());
			dto.setStatus("FAIL");
		}
		return dto;
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

	/** True when the named field is both a handle and schema type {@code array}. */
	public static boolean schemaFieldIsArrayHandle(String fieldName) {
		if (fieldName == null || fieldName.isBlank()) {
			return false;
		}
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		if (props == null || !props.has(fieldName)) {
			return false;
		}
		JSONObject fieldDef = props.getJSONObject(fieldName);
		return fieldDef.optBoolean("handle", false) && "array".equals(fieldDef.optString("type", "string"));
	}

	/** Update/add cases that rewrite email as [{value, tags}], not as a string. */
	private static boolean testRequiresArrayTypedEmail(String testCaseName) {
		return testCaseName.contains("_WithMultipleEmail")
				|| testCaseName.contains("_RetainTaggedEmail")
				|| testCaseName.contains("_WithLengthyEmailArray")
				|| testCaseName.contains("_WithMultipleHandlesDifferentDomains")
				|| testCaseName.contains("_WithEmailHandleAndNotification")
				|| testCaseName.contains("_OnlyNotificationTag")
				|| testCaseName.contains("_SelectedHandlesOrderVariation");
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

	/**
	 * Schema-driven AddIdentity templates put {@code documentType} metadata on {@code identity}
	 * (e.g. proofOfIdentity) but often only put {@code individualBiometrics} in {@code documents}.
	 * Id-repo writes {@code uin_document} / {@code uin_document_h} only for non-bio categories that
	 * appear in {@code request.documents} (same pattern as the local JMeter script).
	 * <p>
	 * For each identity field whose schema {@code $ref} is {@code documentType}, ensure a matching
	 * {@code documents[]} entry exists with a small URL-safe Base64 payload.
	 *
	 * @param inputJson rendered Add/Update Identity request body
	 * @return body with demographic document binaries added when missing
	 */
	public static String ensureDemographicDocumentsInRequest(String inputJson) {
		if (inputJson == null || inputJson.isBlank()) {
			return inputJson;
		}
		try {
			JSONObject root = new JSONObject(inputJson);
			if (!root.has("request")) {
				return inputJson;
			}
			JSONObject request = root.getJSONObject("request");
			if (!request.has("identity")) {
				return inputJson;
			}
			JSONObject identity = request.getJSONObject("identity");
			JSONArray documents = request.optJSONArray("documents");
			if (documents == null) {
				documents = new JSONArray();
				request.put("documents", documents);
			}

			java.util.Set<String> existingCategories = new java.util.HashSet<>();
			for (int i = 0; i < documents.length(); i++) {
				existingCategories.add(documents.getJSONObject(i).optString("category", ""));
			}

			JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
			if (props == null) {
				return inputJson;
			}

			// Same placeholder bytes JMeter uses for proof docs (decoded: test_doc)
			final String demoDocValue = "dGVzdF9kb2M";
			int added = 0;
			List<String> fieldNames = new ArrayList<>(props.keySet());
			Collections.sort(fieldNames);
			for (String fieldName : fieldNames) {
				JSONObject fieldDef = props.optJSONObject(fieldName);
				if (fieldDef == null) {
					continue;
				}
				String ref = fieldDef.optString("$ref", "");
				if (!ref.contains("documentType")) {
					continue;
				}
				if (!identity.has(fieldName) || existingCategories.contains(fieldName)) {
					continue;
				}
				JSONObject doc = new JSONObject();
				doc.put("category", fieldName);
				doc.put("value", demoDocValue);
				documents.put(doc);
				existingCategories.add(fieldName);
				added++;
			}
			if (added > 0) {
				logger.info("Added " + added + " demographic document(s) to request.documents for uin_document coverage");
			}
			return root.toString();
		} catch (Exception e) {
			logger.warn("ensureDemographicDocumentsInRequest skipped: " + e.getMessage());
			return inputJson;
		}
	}

}