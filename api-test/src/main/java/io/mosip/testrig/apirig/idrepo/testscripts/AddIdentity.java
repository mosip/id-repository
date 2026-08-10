package io.mosip.testrig.apirig.idrepo.testscripts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class AddIdentity extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(AddIdentity.class);
	protected String testCaseName = "";
	public Response response = null;

	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;

	}

	@BeforeClass
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
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
			uin = JsonPrecondtion.getValueFromJson(
					RestClient.getRequestWithCookie(ApplnURI + "/v1/idgenerator/uin", MediaType.APPLICATION_JSON,
							MediaType.APPLICATION_JSON, COOKIENAME,
							new KernelAuthentication().getTokenByRole(testCaseDTO.getRole())).asString(),
					"response.uin");
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
		String phoneNumber = "";
		String email = testCaseName + "_" + BaseTestCase.runContext + "@mosip.net";
		if (inputJson.contains("$PHONENUMBERFORIDENTITY$") || inputJson.contains("$EMAILVALUE$")) {
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
		// Resolve $HANDLEVALUE tokens before IdRepoArrayHandle runs so its mutations see real values.
		inputJson = IdRepoUtil.resolveGenericHandleValueTokens(inputJson);

		// V1 handling for verifiedAttribute (plain list of fieldId strings, e.g. ["fullName","address"]).
		// V1 requests are schema-driven with no verifiedAttribute placeholder, so it is injected as a fixed
		// top-level request field here, mirroring how verifiedAttributes is handled for V2 (see wrapV2()).
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

		response = postWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName());

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
	 * The method ser current test name to result
	 * 
	 * @param result
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		result.setAttribute("TestCaseName", testCaseName);
	}

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
