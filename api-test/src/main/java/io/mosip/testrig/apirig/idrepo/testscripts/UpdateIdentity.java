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
import io.mosip.testrig.apirig.idrepo.utils.IdRepoConfigManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoUtil;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

/**
 * TestNG script for the identity update APIs: {@code PATCH /idrepository/v1/identity/} (v1) and the
 * v2 identity endpoint ({@code GlobalConstants.ADD_IDENTITY_V2_ENDPOINT}), used by
 * {@code UpdateIdentity.yml} and {@code UpdateIdentityBlockedHandleCheck.yml}.
 * <p>
 * Uses the static HBS template from the YAML, adjusted to the live schema and auth-actuator field
 * names. The update can be followed by an optional send-OTP call, defined by the YAML
 * {@code sendOtp} / {@code sendOtpResp} blocks.
 */
public class UpdateIdentity extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(UpdateIdentity.class);
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
	 * TestNG entry point: applies {@link IdRepoUtil#isTestCaseValidForExecution}, then delegates to
	 * {@link #updateIdentity(TestCaseDTO)}.
	 *
	 * @param testCaseDTO the YAML case
	 * @throws AuthenticationTestException on token generation failure
	 * @throws AdminTestException          when validation fails
	 * @throws SecurityXSSException        when the XSS header check is enabled and fails
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseName = IdRepoUtil.isTestCaseValidForExecution(testCaseDTO);
		updateIdentity(testCaseDTO);

	}

	/**
	 * Builds, sends and validates one update-identity request.
	 * <ol>
	 * <li>Skips when the environment failed its health check, or when a VID case runs where VID is
	 * not supported.</li>
	 * <li>Moves any {@code sendOtp} block out of the input and any {@code sendOtpResp} block out of
	 * the output, for the optional OTP step at the end.</li>
	 * <li>Renders the YAML template. For v2 it copies the YAML {@code verifiedAttributes},
	 * {@code documents} and {@code fullName} into the request; for v1 it copies the YAML
	 * {@code verifiedAttribute} array.</li>
	 * <li>Renames {@code phone} and {@code email} keys to the actuator property names and resolves
	 * {@code $RID$}, {@code $PHONENUMBERFORIDENTITY$}, {@code $EMAILVALUE$}, {@code $PRIMARYLANG$}
	 * and {@code $OPTIONALSCHEMAFIELD$} (an optional, claimable schema field; skipped when none
	 * exists).</li>
	 * <li>For {@code IdRepository_} and {@code Auth_} cases, removes date of birth, email or phone
	 * when the schema does not require them, and skips cases named after the removed field.</li>
	 * <li>For {@code _sid} cases, caches the phone as {@code $ID:<testCaseName>_phone$}.</li>
	 * <li>PATCHes the request and asserts the output validation.</li>
	 * <li>If a {@code sendOtp} block was present, posts the OTP request (signed) and validates it
	 * against {@code sendOtpResp}.</li>
	 * </ol>
	 *
	 * @param testCaseDTO the YAML case; its input and output are changed in place when OTP blocks
	 *                    are present
	 * @throws AuthenticationTestException on token generation failure
	 * @throws AdminTestException          when OTP output validation fails
	 * @throws SecurityXSSException        when the XSS header check is enabled and fails
	 * @throws AssertionError              when update output validation fails
	 */
	public void updateIdentity(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException {

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

		JSONObject req = new JSONObject(testCaseDTO.getInput());

		JSONObject otpReqJson = null;
		String otpRequest = null;
		String sendOtpReqTemplate = null;
		String sendOtpEndPoint = null;
		if (req.has(GlobalConstants.SENDOTP)) {
			otpRequest = req.get(GlobalConstants.SENDOTP).toString();
			req.remove(GlobalConstants.SENDOTP);
			otpReqJson = new JSONObject(otpRequest);
			sendOtpReqTemplate = otpReqJson.getString("sendOtpReqTemplate");
			otpReqJson.remove("sendOtpReqTemplate");
			sendOtpEndPoint = otpReqJson.getString("sendOtpEndPoint");
			otpReqJson.remove("sendOtpEndPoint");
			testCaseDTO.setInput(req.toString());

		}
		JSONObject res = new JSONObject(testCaseDTO.getOutput());
		String sendOtpResp = null, sendOtpResTemplate = null;
		if (res.has(GlobalConstants.SENDOTPRESP)) {
			sendOtpResp = res.get(GlobalConstants.SENDOTPRESP).toString();
			res.remove(GlobalConstants.SENDOTPRESP);
			testCaseDTO.setOutput(res.toString());
		}

		DateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		String timestampValue = dateFormatter.format(cal.getTime());
		String genRid = "27847" + generateRandomNumberString(10) + timestampValue;
		generatedRid = genRid;

		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());
		if (inputJson.contains("$SCHEMAVERSION$"))
			inputJson = replaceKeywordWithValue(inputJson, "$SCHEMAVERSION$", generateLatestSchemaVersion());
		
		// V2 specific handling for verifiedAttributes, documents and fullName
		if (testCaseDTO.getEndPoint().contains(GlobalConstants.ADD_IDENTITY_V2_ENDPOINT)) {

		    JSONObject originalInput = new JSONObject(testCaseDTO.getInput());
		    JSONObject requestJson = new JSONObject(inputJson);

		    if (originalInput.has("verifiedAttributes")) {
		        // Generic Object getter, since verifiedAttributes may be a list or a map.
		        requestJson.getJSONObject("request")
		                .put("verifiedAttributes", originalInput.get("verifiedAttributes"));
		    }

		    if (originalInput.has("documents")) {
		        requestJson.getJSONObject("request")
		                .put("documents",
		                        originalInput.getJSONArray("documents"));
		    }

		    if (originalInput.has("fullName")) {
		        // fullName isn't in the static V2 template, so inject it here like verifiedAttributes/documents.
		        requestJson.getJSONObject("request").getJSONObject("identity")
		                .put("fullName", originalInput.get("fullName"));
		    }

		    inputJson = requestJson.toString();
		} else {
			// V1's static template has no verifiedAttribute placeholder, so inject it as a fixed top-level field.
			JSONObject originalInput = new JSONObject(testCaseDTO.getInput());
			if (originalInput.has("verifiedAttribute")) {
				JSONObject requestJson = new JSONObject(inputJson);
				requestJson.getJSONObject("request").put("verifiedAttribute",
						originalInput.getJSONArray("verifiedAttribute"));
				inputJson = requestJson.toString();
			}
		}

		String phone = getValueFromAuthActuator("json-property", "phone_number");
		String result = phone.replaceAll("\\[\"|\"\\]", "");

		String email = getValueFromAuthActuator("json-property", "emailId");
		String emailResult = email.replaceAll("\\[\"|\"\\]", "");

		JSONArray dobArray = new JSONArray(getValueFromAuthActuator("json-property", "dob"));
		String dob = dobArray.getString(0);

		inputJson = inputJson.replace("\"phone\":", "\"" + result + "\":");
		inputJson = inputJson.replace("\"email\":", "\"" + emailResult + "\":");

		inputJson = inputJson.replace("$RID$", genRid);

		String generatedEmail = testCaseName + "_" + BaseTestCase.runContext + timestampValue + "@mosip.net";
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
			inputJson = replaceKeywordWithValue(inputJson, "$EMAILVALUE$", generatedEmail);
		}

		if (inputJson.contains("$OPTIONALSCHEMAFIELD$")) {
			// Resolved from the live schema; isTestCaseValidForExecution already skips if none exists, so this is just a fallback.
			String optionalField = resolveOptionalClaimableField();
			if (optionalField == null) {
				throw new SkipException("No optional (non-required) claimable field in the current IdSchema — "
						+ GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
			}
			inputJson = inputJson.replace("$OPTIONALSCHEMAFIELD$", optionalField);
		}

		if ((testCaseName.startsWith("IdRepository_") || testCaseName.startsWith("Auth_"))
				&& inputJson.contains("dateOfBirth") && (!isElementPresent(globalRequiredFields, dob))) {
			JSONObject reqJson = new JSONObject(inputJson);
			reqJson.getJSONObject("request").getJSONObject("identity").remove("dateOfBirth");
			inputJson = reqJson.toString();
			if (testCaseName.contains("dob"))
				throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		if ((testCaseName.startsWith("IdRepository_") || testCaseName.startsWith("Auth_"))
				&& inputJson.contains("email")
				&& (!isElementPresent(globalRequiredFields, emailResult))) {
			JSONObject reqJson = new JSONObject(inputJson);
			reqJson.getJSONObject("request").getJSONObject("identity").remove(emailResult);
			if (reqJson.getJSONObject("request").getJSONObject("identity").has(result)) {
				reqJson.getJSONObject("request").getJSONObject("identity").remove(result);
			}
			if (testCaseName.contains("email") || testCaseName.contains("phonenumber"))
				throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);

			inputJson = reqJson.toString();
		}

		if (inputJson.contains("$PRIMARYLANG$"))
			inputJson = inputJson.replace("$PRIMARYLANG$", BaseTestCase.languageList.get(0));
		
		inputJson = inputJsonKeyWordHandeler(inputJson, testCaseName);
		if (testCaseName.toLowerCase().contains("_sid")) {
			JSONObject reqJson = new JSONObject(inputJson);
			String phonenumber = reqJson.getJSONObject("request").getJSONObject("identity").getString("phone");
			writeToCache(getAutogenIdKeyName(testCaseName, "phone"), phonenumber);
		}
		
		Response response = patchWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName());

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()),
				testCaseDTO, response.getStatusCode());
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));
		Assert.assertEquals(OutputValidationUtil.publishOutputResult(ouputValid), true);

		if (otpReqJson != null) {
			Response otpResponse = null;
			otpResponse = postRequestWithAuthHeaderAndSignature(ApplnURI + sendOtpEndPoint,
					getJsonFromTemplate(otpReqJson.toString(), sendOtpReqTemplate), testCaseDTO.getTestCaseName());

			JSONObject sendOtpRespJson = new JSONObject(sendOtpResp);
			sendOtpResTemplate = sendOtpRespJson.getString("sendOtpResTemplate");
			sendOtpRespJson.remove("sendOtpResTemplate");
			Map<String, List<OutputValidationDto>> ouputValidOtp = OutputValidationUtil.doJsonOutputValidation(
					otpResponse.asString(), getJsonFromTemplate(sendOtpRespJson.toString(), sendOtpResTemplate),
					testCaseDTO, otpResponse.getStatusCode());
			Reporter.log(ReportUtil.getOutputValidationReport(ouputValidOtp));

			if (!OutputValidationUtil.publishOutputResult(ouputValidOtp))
				throw new AdminTestException("Failed at Send OTP output validation");
		}
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
