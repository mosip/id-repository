package io.mosip.testrig.apirig.idrepo.testscripts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoArrayHandle;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoConfigManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoUtil;
import io.mosip.testrig.apirig.utils.SchemaBasedIdentityTemplateBuilder;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.testrunner.JsonPrecondtion;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.KernelAuthentication;
import io.mosip.testrig.apirig.utils.KeycloakUserManager;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.RestClient;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

/**
 * TestNG script for the identity create APIs: {@code POST /idrepository/v1/identity/} (v1) and the v2
 * add-identity endpoint ({@link GlobalConstants#ADD_IDENTITY_V2_ENDPOINT}).
 * <p>
 * The request body is built from the live IdSchema rather than a static HBS file, so the same
 * YAML works for any country schema. Placeholders resolved here:
 * <ul>
 * <li>{@code $UIN$}: a UIN from {@code /v1/idgenerator/uin} not yet used in this run; on
 * {@code IDR-IDC-012} the request is resent with a new one.</li>
 * <li>{@code $RID$}: {@code 27847} + 10 random digits + {@code yyyyMMddHHmmss}.</li>
 * <li>{@code $FUNCTIONALID$}, {@code $PHONENUMBERFORIDENTITY$} (matches the schema phone regex
 * when one is defined) and {@code $EMAILVALUE$} ({@code <testCaseName>_<runContext><timestamp>@mosip.net}).</li>
 * <li>{@code $HANDLEVALUE:<field>$}: a value valid for that schema field.</li>
 * </ul>
 * Handle scenarios are applied through {@link IdRepoArrayHandle}. Demographic documents are added
 * so {@code uin_document} rows are written.
 * <p>
 * For {@code _Pos} cases the generated UIN, RID and EMAIL (and PHONE, whenever one was generated)
 * are cached as {@code $ID:<testCaseName>_UIN$} and so on, for later YAML files.
 */
public class AddIdentity extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(AddIdentity.class);
	/** Error code returned when the UIN already has an identity. */
	private static final String UIN_EXISTS_ERROR = "IDR-IDC-012";
	/** Generator calls per {@code $UIN$} before accepting a UIN already used in this run. */
	private static final int MAX_UIN_FETCH_ATTEMPTS = 20;
	/** Resends with a fresh UIN when the service still reports {@link #UIN_EXISTS_ERROR}. */
	private static final int MAX_UIN_EXISTS_RETRIES = 3;
	/** UINs handed to add-identity requests during this run. */
	private static final Set<String> USED_UINS = ConcurrentHashMap.newKeySet();
	/** Name of the case currently running; becomes the TestNG method name in reports. */
	protected String testCaseName = "";
	/** Response of the last add-identity call. */
	public Response response = null;

	/**
	 * @return the current test case name (TestNG {@link ITest} contract)
	 */
	@Override
	public String getTestName() {
		return testCaseName;

	}

	/** Sets the script logger to {@code ALL} in debug mode, {@code ERROR} otherwise. */
	@BeforeClass
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	/**
	 * Supplies the YAML cases named by the suite parameter {@code ymlFile}.
	 *
	 * @param context TestNG context holding the {@code <test>} parameters
	 * @return one {@link TestCaseDTO} per YAML entry
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Runs one add-identity case.
	 * <ol>
	 * <li>Applies {@link IdRepoUtil#isTestCaseValidForExecution} and skips when the target
	 * environment failed its health check.</li>
	 * <li>Builds the v1 or v2 schema-based template, chosen by the endpoint, and resolves the
	 * placeholders listed on the class.</li>
	 * <li>For {@code Resident_AddIdentity_Valid_Params_AddUser_smoke_Pos}, recreates the Keycloak
	 * resident user linked to the new UIN.</li>
	 * <li>v1 only: copies the YAML {@code verifiedAttribute} array into the request.</li>
	 * <li>Applies handle scenarios when {@code selectedHandles} is present, plus
	 * {@code _extraNonSchemaField}, and adds missing demographic documents.</li>
	 * <li>POSTs the request and validates the response against the YAML output template.</li>
	 * <li>Caches the generated ids for positive cases.</li>
	 * </ol>
	 *
	 * @param testCaseDTO the YAML case
	 * @throws AuthenticationTestException on token generation failure
	 * @throws AdminTestException          when output validation fails
	 * @throws SecurityXSSException        when the XSS header check is enabled and fails
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseName = IdRepoUtil.isTestCaseValidForExecution(testCaseDTO);
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException(
					GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED + HealthChecker.healthCheckFailureMapS);
		}
		// modifySchemaGenerateHbs is called only to populate schema globals used below; its template is replaced.
		if(testCaseDTO.getEndPoint().contains(GlobalConstants.ADD_IDENTITY_V2_ENDPOINT)) {
			AdminTestUtil.modifySchemaGenerateHbsV2(testCaseDTO.isRegenerateHbs());
			testCaseDTO.setInputTemplate(SchemaBasedIdentityTemplateBuilder.buildAddIdentityTemplateV2());
		} else {
			AdminTestUtil.modifySchemaGenerateHbs(testCaseDTO.isRegenerateHbs());
			testCaseDTO.setInputTemplate(SchemaBasedIdentityTemplateBuilder.buildAddIdentityTemplate());
		}

		String jsonInput = testCaseDTO.getInput();

		String inputJson = getJsonFromTemplate(jsonInput, testCaseDTO.getInputTemplate(), false);
		String uin = null;
		if (inputJson.contains("$UIN$")) {
			uin = fetchUnusedUin(testCaseDTO.getRole());
		}

		DateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		String timestampValue = dateFormatter.format(cal.getTime());
		String genRid = "27847" + generateRandomNumberString(10) + timestampValue;

		if (testCaseName.equals("Resident_AddIdentity_Valid_Params_AddUser_smoke_Pos")) {

			KeycloakUserManager.removeVidUser();
			Map<String, List<String>> attrmap = new HashMap<>();
			List<String> list = new ArrayList<>();
			if (uin == null) {
		        throw new IllegalStateException("UIN not initialized");
		    }
			list.add(uin);
			attrmap.put("individual_id", list);
			list = new ArrayList<>();
			String token = AdminTestUtil.generateTokenID(uin, properties.getProperty("partner_Token_Id"));
			list.add(token);
			attrmap.put("ida_token", list);
			list = new ArrayList<>();
			String picture = properties.getProperty("picturevalue");
			list.add(picture);
			attrmap.put("picture", list);
			KeycloakUserManager.createVidUsers(IdRepoConfigManager.getproperty("new_Resident_User"), attrmap);
		}

		//For_Array-Handle Related Cases
		if (inputJson.contains("$FUNCTIONALID$")) {
			inputJson = replaceKeywordWithValue(inputJson, "$FUNCTIONALID$", generateRandomNumberString(2)
					+ Calendar.getInstance().getTimeInMillis());
		}
		
		if (uin != null) {
			inputJson = inputJson.replace("$UIN$", uin);
		}
		inputJson = inputJson.replace("$RID$", genRid);
		// Applied only when YAML/template still contains the token. A YAML address is left as-is.
		// runContext already ends with '_'; schema local-part allows [A-Za-z0-9_-].
		String email = testCaseName + "_" + BaseTestCase.runContext + timestampValue + "@mosip.net";
		String phoneNumber = "";
		if (inputJson.contains("$PHONENUMBERFORIDENTITY$") || inputJson.contains("$EMAILVALUE$")) {
			if (!phoneSchemaRegex.isEmpty()) {
				try {
					phoneNumber = genStringAsperRegex(phoneSchemaRegex);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
			if (phoneNumber.isEmpty()) {
				phoneNumber = timestampValue.substring(timestampValue.length() - 6)
						+ generateRandomNumberString(4);
			}
			inputJson = replaceKeywordWithValue(inputJson, "$PHONENUMBERFORIDENTITY$", phoneNumber);
		}
		if (inputJson.contains("$EMAILVALUE$")) {
			inputJson = replaceKeywordWithValue(inputJson, "$EMAILVALUE$", email);
		}
		// Resolve $HANDLEVALUE tokens before IdRepoArrayHandle runs so its mutations see real values.
		inputJson = IdRepoUtil.resolveGenericHandleValueTokens(inputJson);

		// V1's schema-driven template has no verifiedAttribute placeholder, so inject it as a fixed top-level field.
		if (!testCaseDTO.getEndPoint().contains(GlobalConstants.ADD_IDENTITY_V2_ENDPOINT)) {
			JSONObject originalAddInput = new JSONObject(jsonInput);
			if (originalAddInput.has("verifiedAttribute")) {
				JSONObject requestJsonForVerifiedAttribute = new JSONObject(inputJson);
				requestJsonForVerifiedAttribute.getJSONObject("request").put("verifiedAttribute",
						originalAddInput.getJSONArray("verifiedAttribute"));
				inputJson = requestJsonForVerifiedAttribute.toString();
			}
		}

		JSONObject jsonString = new JSONObject(inputJson);
		if (jsonString.getJSONObject("request").getJSONObject("identity").has("selectedHandles")) {
			inputJson = IdRepoArrayHandle.replaceArrayHandleValues(inputJson, testCaseName);
		}
		// Done here (not the handle dispatch) so it also runs on schemas with no handles.
		if (testCaseName.contains("_extraNonSchemaField")) {
			inputJson = IdRepoArrayHandle.injectExtraNonSchemaField(inputJson);
		}

		// Schema identity includes proofOf* metadata but documents[] is usually bio-only;
		// add demographic binaries so id-repo persists uin_document / uin_document_h (JMeter parity).
		inputJson = IdRepoUtil.ensureDemographicDocumentsInRequest(inputJson);

		response = postWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName());

		boolean duplicateUinExpected = String.valueOf(testCaseDTO.getOutput()).contains(UIN_EXISTS_ERROR);
		for (int retry = 1; uin != null && !duplicateUinExpected && retry <= MAX_UIN_EXISTS_RETRIES
				&& response.asString().contains(UIN_EXISTS_ERROR); retry++) {
			String newUin = fetchUnusedUin(testCaseDTO.getRole());
			logger.warn(testCaseName + ": UIN " + uin + " already exists (" + UIN_EXISTS_ERROR + "); retry " + retry
					+ " with " + newUin);
			inputJson = inputJson.replace("\"" + uin + "\"", "\"" + newUin + "\"");
			uin = newUin;
			response = postWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
					testCaseDTO.getRole(), testCaseDTO.getTestCaseName());
		}

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()),
				testCaseDTO, response.getStatusCode());
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

		if (!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AdminTestException("Failed at output validation");
		if (testCaseDTO.getTestCaseName().contains("_Pos")) {
			writeAutoGeneratedId(testCaseDTO.getTestCaseName(), "UIN", uin);
			writeAutoGeneratedId(testCaseDTO.getTestCaseName(), "RID", genRid);
			writeAutoGeneratedId(testCaseDTO.getTestCaseName(), "EMAIL", email);
		}
		if (!phoneNumber.isEmpty())
			writeAutoGeneratedId(testCaseDTO.getTestCaseName(), "PHONE", phoneNumber);
	}

	/**
	 * Fetches a UIN from {@code /v1/idgenerator/uin} that this run has not used yet.
	 * <p>
	 * Local WireMock picks randomly from a fixed pool of 5000 UINs, so within one run two cases
	 * can draw the same UIN and the second fails with {@code IDR-IDC-012}. Up to
	 * {@value #MAX_UIN_FETCH_ATTEMPTS} draws are made; after that the last one is returned.
	 * </p>
	 *
	 * @param role role whose token calls the generator
	 * @return a UIN, recorded as used
	 */
	private String fetchUnusedUin(String role) {
		String uin = null;
		for (int attempt = 1; attempt <= MAX_UIN_FETCH_ATTEMPTS; attempt++) {
			uin = JsonPrecondtion.getValueFromJson(
					RestClient.getRequestWithCookie(ApplnURI + "/v1/idgenerator/uin", MediaType.APPLICATION_JSON,
							MediaType.APPLICATION_JSON, COOKIENAME, new KernelAuthentication().getTokenByRole(role))
							.asString(),
					"response.uin");
			if (uin == null || uin.isBlank() || USED_UINS.add(uin)) {
				return uin;
			}
			logger.info("UIN " + uin + " already used in this run; fetching another (attempt " + attempt + ")");
		}
		logger.warn("No unused UIN after " + MAX_UIN_FETCH_ATTEMPTS + " attempts; using " + uin);
		return uin;
	}

	/**
	 * Stores the case name as the {@code TestCaseName} result attribute, which the report uses as the
	 * row label.
	 *
	 * @param result result of the method that just ran
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		result.setAttribute("TestCaseName", testCaseName);
	}

	/**
	 * When the run module is {@code auth} or {@code esignet}, waits {@code Delaytime} ms after the
	 * class so new identities can propagate to IDA before dependent suites start.
	 */
	@AfterClass(alwaysRun = true)
	public void waittime() {

		try {
			if (BaseTestCase.currentModule.equals("auth") || BaseTestCase.currentModule.equals("esignet")) {
				logger.info("waiting for " + properties.getProperty("Delaytime")
						+ " mili secs after UIN Generation In IDREPO"); //
				Thread.sleep(Long.parseLong(properties.getProperty("Delaytime")));
			}
		} catch (Exception e) {
			logger.error("Exception : " + e.getMessage());
			Thread.currentThread().interrupt();
		}

	}
}
