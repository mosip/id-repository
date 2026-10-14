package io.mosip.testrig.apirig.idrepo.testscripts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;
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
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

/**
 * TestNG script for the handle scenarios of the identity update APIs (v1 and v2), used by
 * {@code UpdateIdentityArrayHandle.yml} and {@code UpdateIdentityV2ArrayHandle.yml}.
 * <p>
 * Unlike {@link UpdateIdentity}, the request template is built from the live IdSchema
 * ({@code SchemaBasedIdentityTemplateBuilder}). The handle scenario named in the test case is then
 * applied by {@link IdRepoArrayHandle#replaceArrayHandleValuesForUpdateIdentity}.
 */
public class UpdateIdentityForArrayHandles extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(UpdateIdentityForArrayHandles.class);
	/** Name of the case currently running; becomes the TestNG method name in reports. */
	protected String testCaseName = "";
	/** Identity value shared with later verification steps; see the accessors below. */
	private static String identity;

	/** Sets the script logger to {@code ALL} in debug mode, {@code ERROR} otherwise. */
	@BeforeClass
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	/**
	 * Stores an identity value (for example a UIN) for a later step to verify. Static, so the value
	 * is visible across script instances in the same run.
	 *
	 * @param id value to remember
	 */
	public static void saveIdentityForUpdateIdentityVerification(String id) {
		identity = id;
	}

	/** @return the value last stored by {@link #saveIdentityForUpdateIdentityVerification(String)}, or null */
	public static String getIdentityForUpdateIdentityVerification() {
		return identity;
	}

	/**
	 * @return the current test case name (TestNG {@link ITest} contract)
	 */
	@Override
	public String getTestName() {
		return testCaseName;
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
	 * Runs one handle update case.
	 * <ol>
	 * <li>Applies {@link IdRepoUtil#isTestCaseValidForExecution}, then builds the v1 or v2
	 * schema-based update template, chosen by the endpoint.</li>
	 * <li>Skips when the environment failed its health check, or when a VID case runs where VID is
	 * not supported.</li>
	 * <li>Resolves {@code $PHONENUMBERFORIDENTITY$}, {@code $EMAILVALUE$}, {@code $RID$} and
	 * {@code $FUNCTIONALID$}; for {@code IdRepository_} cases removes date of birth when the schema
	 * does not require it.</li>
	 * <li>For {@code _sid} cases, caches the phone as {@code $ID:<testCaseName>_phone$}.</li>
	 * <li>Resolves {@code $HANDLEVALUE:<field>$} tokens, then applies the handle scenario.</li>
	 * <li>PATCHes the request and asserts the output validation.</li>
	 * </ol>
	 *
	 * @param testCaseDTO the YAML case
	 * @throws AuthenticationTestException on token generation failure
	 * @throws AdminTestException          declared by the shared helpers
	 * @throws SecurityXSSException        when the XSS header check is enabled and fails
	 * @throws AssertionError              when output validation fails
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseName = IdRepoUtil.isTestCaseValidForExecution(testCaseDTO);
		
		// updateIdentityHbs is called only to populate schema globals used below; its template is replaced.
		if(testCaseDTO.getEndPoint().contains(GlobalConstants.ADD_IDENTITY_V2_ENDPOINT)) {
			AdminTestUtil.updateIdentityHbsV2(testCaseDTO.isRegenerateHbs());
			testCaseDTO.setInputTemplate(SchemaBasedIdentityTemplateBuilder.buildUpdateIdentityTemplateV2());
		} else {
			AdminTestUtil.updateIdentityHbs(testCaseDTO.isRegenerateHbs());
			testCaseDTO.setInputTemplate(SchemaBasedIdentityTemplateBuilder.buildUpdateIdentityTemplate());
		}
		testCaseName = testCaseDTO.getTestCaseName();
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException(
					GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED + HealthChecker.healthCheckFailureMapS);
		}

		if (testCaseDTO.getTestCaseName().contains("VID") || testCaseDTO.getTestCaseName().contains("Vid")) {
			if (!BaseTestCase.getSupportedIdTypesValueFromActuator().contains("VID")
					&& !BaseTestCase.getSupportedIdTypesValueFromActuator().contains("vid")) {
				throw new SkipException(GlobalConstants.VID_FEATURE_NOT_SUPPORTED);
			}
		}

		DateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		String timestampValue = dateFormatter.format(cal.getTime());
		String genRid = "27847" + generateRandomNumberString(10) + timestampValue;
		generatedRid = genRid;

		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate(), false);

		JSONArray dobArray = new JSONArray(getValueFromAuthActuator("json-property", "dob"));
		String dob = dobArray.getString(0);
		String phoneNumber = "";
		String email = testCaseName + "_" + BaseTestCase.runContext + timestampValue + "@mosip.net";
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
		
		

		inputJson = inputJson.replace("$RID$", genRid);

		if ((testCaseName.startsWith("IdRepository_")) && inputJson.contains("dateOfBirth")
				&& (!isElementPresent(globalRequiredFields, dob))) {
			JSONObject reqJson = new JSONObject(inputJson);
			reqJson.getJSONObject("request").getJSONObject("identity").remove("dateOfBirth");
			inputJson = reqJson.toString();
			if (testCaseName.contains("dob"))
				throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		if (inputJson.contains("$FUNCTIONALID$")) {
			inputJson = replaceKeywordWithValue(inputJson, "$FUNCTIONALID$", generateRandomNumberString(2)
					+ Calendar.getInstance().getTimeInMillis());
		}
		inputJson = inputJsonKeyWordHandeler(inputJson, testCaseName);
		if (testCaseName.toLowerCase().contains("_sid")) {
			JSONObject reqJson = new JSONObject(inputJson);
			String phonenumber = reqJson.getJSONObject("request").getJSONObject("identity").getString("phone");
			writeToCache(getAutogenIdKeyName(testCaseName, "phone"), phonenumber);
		}

		// Resolve $HANDLEVALUE tokens before IdRepoArrayHandle runs so its mutations see real values.
		inputJson = IdRepoUtil.resolveGenericHandleValueTokens(inputJson);
		inputJson = IdRepoArrayHandle.replaceArrayHandleValuesForUpdateIdentity(inputJson, testCaseName);

		Response response = patchWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName());

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()),
				testCaseDTO, response.getStatusCode());
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));
		Assert.assertEquals(OutputValidationUtil.publishOutputResult(ouputValid), true);

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
}
