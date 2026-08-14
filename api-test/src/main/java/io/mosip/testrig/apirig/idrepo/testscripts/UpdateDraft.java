package io.mosip.testrig.apirig.idrepo.testscripts;

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

// PATCH .../draft/update/{registrationId} and .../draft/v2/update/{registrationId} - same request
// schema on both, so one class serves both YAML files; endpoint is purely YAML-driven, same as
// UpdateIdentity.java already does for AddIdentity/AddIdentityV2.
// Omit inputTemplate for the normal schema-driven body; name a real .hbs path only to override
// it with a curated static payload (needed for negative tests like an invalid email).
public class UpdateDraft extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(UpdateDraft.class);
	protected String testCaseName = "";
	String pathParams = null;
	public Response response = null;

	@BeforeClass
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	/** get current testcaseName */
	@Override
	public String getTestName() {
		return testCaseName;
	}

	/** Data provider class provides test case list */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		pathParams = context.getCurrentXmlTest().getLocalParameters().get("pathParams");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

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
		if (declaredTemplate == null || declaredTemplate.isEmpty()) {
			testCaseDTO.setInputTemplate(AdminTestUtil.generateHbsForUpdateDraft());
			inputJson = getJsonFromTemplate(jsonInput, testCaseDTO.getInputTemplate(), false);
			// No verifiedAttributes placeholder in the generated template - inject it if supplied.
			JSONObject originalInput = new JSONObject(jsonInput);
			if (originalInput.has("verifiedAttributes")) {
				JSONObject requestJson = new JSONObject(inputJson);
				requestJson.getJSONObject("request").put("verifiedAttributes",
						originalInput.get("verifiedAttributes"));
				inputJson = requestJson.toString();
			}
		} else {
			inputJson = getJsonFromTemplate(jsonInput, declaredTemplate);
			// Retarget hardcoded "email"/"phone" keys onto the live schema's actual field names.
			String actualPhoneField = getValueFromAuthActuator("json-property", "phone_number")
					.replaceAll("\\[\"|\"]", "");
			String actualEmailField = getValueFromAuthActuator("json-property", "emailId")
					.replaceAll("\\[\"|\"]", "");
			inputJson = inputJson.replace("\"phone\":", "\"" + actualPhoneField + "\":");
			inputJson = inputJson.replace("\"email\":", "\"" + actualEmailField + "\":");
		}

		if (inputJson.contains("$SCHEMAVERSION$"))
			inputJson = replaceKeywordWithValue(inputJson, "$SCHEMAVERSION$", generateLatestSchemaVersion());

		String phoneNumber = "";
		String email = testCaseName + "_" + BaseTestCase.runContext + "@mosip.net";
		if (inputJson.contains("$PHONENUMBERFORIDENTITY$") || inputJson.contains("$EMAILVALUE$")) {
			if (!phoneSchemaRegex.isEmpty()) {
				try {
					phoneNumber = genStringAsperRegex(phoneSchemaRegex);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
			inputJson = replaceKeywordWithValue(inputJson, "$PHONENUMBERFORIDENTITY$", phoneNumber);
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

	/** Sets current test name to result */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		result.setAttribute("TestCaseName", testCaseName);
	}

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
