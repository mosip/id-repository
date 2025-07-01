package io.mosip.testrig.apirig.idrepo.testscripts;

import java.lang.reflect.Field;
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
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoArrayHandle;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoConfigManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoUtil;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
//import io.mosip.testrig.apirig.utils.PartnerRegistration;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

public class UpdateIdentityForArrayHandles extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(UpdateIdentityForArrayHandles.class);
	protected String testCaseName = "";
	private static String identity;

	@BeforeClass
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	public static void saveIdentityForUpdateIdentityVerification(String id) {
		identity = id;
	}

	public static String getIdentityForUpdateIdentityVerification() {
		return identity;
	}

	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseName = IdRepoUtil.isTestCaseValidForExecution(testCaseDTO);


		testCaseDTO.setInputTemplate(AdminTestUtil.updateIdentityHbs(testCaseDTO.isRegenerateHbs()));
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
		String email = testCaseName +"@mosip.net";
		
		
		if (inputJson.contains("$PHONENUMBERFORIDENTITY$")) {
			if (!phoneSchemaRegex.isEmpty())
				try {
					phoneNumber = genStringAsperRegex(phoneSchemaRegex);
				} catch (Exception e) {
					logger.error(e.getMessage());
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

		JSONObject jsonString = new JSONObject(inputJson);
		if (jsonString.getJSONObject("request").getJSONObject("identity").has("selectedHandles")) {
			inputJson = IdRepoArrayHandle.replaceArrayHandleValuesForUpdateIdentity(inputJson,testCaseName);
		}

		Response response = patchWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName());

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()),
				testCaseDTO, response.getStatusCode());
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));
		Assert.assertEquals(OutputValidationUtil.publishOutputResult(ouputValid), true);

	}

	/**
	 * The method ser current test name to result
	 * 
	 * @param result
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
}
