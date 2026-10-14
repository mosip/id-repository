package io.mosip.testrig.apirig.idrepo.testscripts;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoConfigManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoUtil;
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
 * TestNG script for the draft update APIs:
 * {@code PATCH /idrepository/v1/identity/draft/update/{registrationId}} ({@code UpdateDraft.yml}) and
 * {@code PATCH /idrepository/v1/identity/draft/v2/update/{registrationId}} ({@code UpdateDraftV2.yml}).
 * <p>
 * Two ways to build the request:
 * <ul>
 * <li>No input template, or one containing {@code $LANGNUMBER$}: the body is generated from the
 * live IdSchema ({@code AdminTestUtil.generateHbsForUpdateDraft()}). The YAML
 * {@code verifiedAttributes} value (a list of strings or of v2 verification-metadata objects) is
 * copied into {@code request} unchanged.</li>
 * <li>An explicit template: rendered as is, then the {@code phone} and {@code email} keys are
 * renamed to the property names published by the auth actuator.</li>
 * </ul>
 * The suite parameter {@code pathParams} (normally {@code registrationId}) moves that field into
 * the URL.
 */
public class UpdateDraft extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(UpdateDraft.class);
	/** Name of the case currently running; becomes the TestNG method name in reports. */
	protected String testCaseName = "";
	/** Suite parameter {@code pathParams}: input fields used as URL path parameters. */
	String pathParams = null;
	/** Response of the last call. */
	public Response response = null;

	/** Sets the script logger to {@code ALL} in debug mode, {@code ERROR} otherwise. */
	@BeforeClass
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	/**
	 * @return the current test case name (TestNG {@link ITest} contract)
	 */
	@Override
	public String getTestName() {
		return testCaseName;
	}

	/**
	 * Supplies the YAML cases named by the suite parameter {@code ymlFile} and reads
	 * {@code pathParams}.
	 *
	 * @param context TestNG context holding the {@code <test>} parameters
	 * @return one {@link TestCaseDTO} per YAML entry
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		pathParams = context.getCurrentXmlTest().getLocalParameters().get("pathParams");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Runs one draft-update case.
	 * <p>
	 * Uses the same skip rules as the other scripts (execution gate, health check, VID support).
	 * Builds the body (see the class description), then resolves {@code $SCHEMAVERSION$},
	 * {@code $PHONENUMBERFORIDENTITY$}, {@code $EMAILVALUE$} and the registration-id placeholders.
	 * Sends the PATCH with {@link #pathParams} and validates the response against the YAML output
	 * template.
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

		if (testCaseDTO.getTestCaseName().contains("VID") || testCaseDTO.getTestCaseName().contains("Vid")) {
			if (!BaseTestCase.getSupportedIdTypesValueFromActuator().contains("VID")
					&& !BaseTestCase.getSupportedIdTypesValueFromActuator().contains("vid")) {
				throw new SkipException(GlobalConstants.VID_FEATURE_NOT_SUPPORTED);
			}
		}
		String jsonInput = testCaseDTO.getInput();
		String inputJson;
		String declaredTemplate = testCaseDTO.getInputTemplate();
		if (declaredTemplate == null || declaredTemplate.isEmpty() || declaredTemplate.contains("$LANGNUMBER$")) {
			testCaseDTO.setInputTemplate(AdminTestUtil.generateHbsForUpdateDraft());
			inputJson = getJsonFromTemplate(jsonInput, testCaseDTO.getInputTemplate(), false);
			JSONObject originalInput = new JSONObject(jsonInput);
			if (originalInput.has("verifiedAttributes")) {
				JSONObject requestJson = new JSONObject(inputJson);
				requestJson.getJSONObject("request").put("verifiedAttributes",
						originalInput.get("verifiedAttributes"));
				inputJson = requestJson.toString();
			}
		} else {
			inputJson = getJsonFromTemplate(jsonInput, declaredTemplate);
			String actualPhoneField = getValueFromAuthActuator("json-property", "phone_number")
					.replaceAll("\\[\"|\"]", "");
			String actualEmailField = getValueFromAuthActuator("json-property", "emailId")
					.replaceAll("\\[\"|\"]", "");
			inputJson = inputJson.replace("\"phone\":", "\"" + actualPhoneField + "\":");
			inputJson = inputJson.replace("\"email\":", "\"" + actualEmailField + "\":");
		}

		if (inputJson.contains("$SCHEMAVERSION$"))
			inputJson = replaceKeywordWithValue(inputJson, "$SCHEMAVERSION$", generateLatestSchemaVersion());

		DateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String timestampValue = dateFormatter.format(Calendar.getInstance().getTime());
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

		inputJson = inputStringKeyWordHandeler(inputJson, testCaseName);

		response = patchWithPathParamsBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), pathParams);

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()),
				testCaseDTO, response.getStatusCode());
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

		if (!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AdminTestException("Failed at output validation");

	}

	/**
	 * Renames the TestNG method on {@code result} to the YAML case name, so each case gets its own
	 * report row instead of {@code test}. Uses reflection on TestNG internals
	 * ({@code TestResult.m_method}, {@code BaseTestMethod.m_methodName}); any failure is written to
	 * the report and ignored.
	 *
	 * @param result result of the method that just ran
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, testCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}

	/**
	 * Waits {@code Delaytime} ms after the class, whatever the module, so draft changes settle
	 * before the publish suites run.
	 */
	@AfterClass(alwaysRun = true)
	public void waittime() {

		try {
			logger.info(
					"waiting for" + properties.getProperty("Delaytime") + " mili secs after UIN Generation In IDREPO");
			Thread.sleep(Long.parseLong(properties.getProperty("Delaytime")));
		} catch (Exception e) {
			logger.error("Exception : " + e.getMessage());
			Thread.currentThread().interrupt();
		}

	}
}