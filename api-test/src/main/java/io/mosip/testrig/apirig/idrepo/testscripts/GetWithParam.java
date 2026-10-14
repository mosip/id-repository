package io.mosip.testrig.apirig.idrepo.testscripts;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
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
 * Generic TestNG script for {@code GET} endpoints whose inputs are path or query parameters, for
 * example retrieve identity by UIN or RID, VID lookups, auth-type status, update count, GetDraftV2
 * and the (GET-based) publish-draft APIs.
 * <p>
 * Besides normal output-template validation it supports two special validation modes, selected by
 * the test case name:
 * <ul>
 * <li>{@code _StatusCode}: only the HTTP status is compared with the YAML {@code output}.</li>
 * <li>{@code UinNotAllocated}: the response must not contain {@code response.identity.UIN}
 * (LOST-packet drafts created with {@code generateUin=false}); see
 * {@link IdRepoUtil#assertDraftUinNotAllocated}.</li>
 * </ul>
 * {@code $PRIMARYLANG$} in input, output or templates becomes the first configured language.
 */
public class GetWithParam extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(GetWithParam.class);
	/** Name of the case currently running; becomes the TestNG method name in reports. */
	protected String testCaseName = "";
	/** Response of the last call. */
	public Response response = null;
	/** True when the suite {@code <test>} declares a {@code sendEsignetToken} parameter. */
	public boolean sendEsignetToken = false;
	/** YAML {@code auditLogCheck} flag of the current case, passed to the REST helper. */
	public boolean auditLogCheck = false;

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
	 * Supplies the YAML cases named by the suite parameter {@code ymlFile} and reads the
	 * {@code sendEsignetToken} flag.
	 *
	 * @param context TestNG context holding the {@code <test>} parameters
	 * @return one {@link TestCaseDTO} per YAML entry
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		sendEsignetToken = context.getCurrentXmlTest().getLocalParameters().containsKey("sendEsignetToken");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Runs one GET case.
	 * <p>
	 * Uses the same skip rules as the other scripts (execution gate, health check, VID support),
	 * then resolves {@code $PRIMARYLANG$}. With YAML {@code templateFields} the call runs once per
	 * configured language. Otherwise it runs once, with registration-id placeholders resolved, and
	 * the response is validated in status-code mode, UIN-not-allocated mode or normal
	 * output-template mode (see the class description).
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
		auditLogCheck = testCaseDTO.isAuditLogCheck();
		String[] templateFields = testCaseDTO.getTemplateFields();

		if (testCaseDTO.getInputTemplate().contains(GlobalConstants.$PRIMARYLANG$))
			testCaseDTO.setInputTemplate(testCaseDTO.getInputTemplate().replace(GlobalConstants.$PRIMARYLANG$,
					BaseTestCase.languageList.get(0)));
		if (testCaseDTO.getOutputTemplate().contains(GlobalConstants.$PRIMARYLANG$))
			testCaseDTO.setOutputTemplate(testCaseDTO.getOutputTemplate().replace(GlobalConstants.$PRIMARYLANG$,
					BaseTestCase.languageList.get(0)));
		if (testCaseDTO.getInput().contains(GlobalConstants.$PRIMARYLANG$))
			testCaseDTO.setInput(
					testCaseDTO.getInput().replace(GlobalConstants.$PRIMARYLANG$, BaseTestCase.languageList.get(0)));
		if (testCaseDTO.getOutput().contains(GlobalConstants.$PRIMARYLANG$))
			testCaseDTO.setOutput(
					testCaseDTO.getOutput().replace(GlobalConstants.$PRIMARYLANG$, BaseTestCase.languageList.get(0)));

		if (testCaseDTO.getTemplateFields() != null && templateFields.length > 0) {
			ArrayList<JSONObject> inputtestCases = AdminTestUtil.getInputTestCase(testCaseDTO);
			ArrayList<JSONObject> outputtestcase = AdminTestUtil.getOutputTestCase(testCaseDTO);
			for (int i = 0; i < languageList.size(); i++) {
				String inputJson = inputStringKeyWordHandeler(
						getJsonFromTemplate(inputtestCases.get(i).toString(), testCaseDTO.getInputTemplate()),
						testCaseName);
				response = getWithPathParamAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson,
						COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName());

				Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
						response.asString(),
						getJsonFromTemplate(outputtestcase.get(i).toString(), testCaseDTO.getOutputTemplate()),
						testCaseDTO, response.getStatusCode());
				Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

				if (!OutputValidationUtil.publishOutputResult(ouputValid))
					throw new AdminTestException("Failed at output validation");
			}
		}

		else {
			String inputJson = inputStringKeyWordHandeler(
					getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate()), testCaseName);
			response = getWithPathParamAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, auditLogCheck,
					COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), sendEsignetToken);
			Map<String, List<OutputValidationDto>> ouputValid = null;
			if (testCaseName.contains("_StatusCode")) {

				OutputValidationDto customResponse = customStatusCodeResponse(String.valueOf(response.getStatusCode()),
						testCaseDTO.getOutput());

				ouputValid = new HashMap<>();
				ouputValid.put(GlobalConstants.EXPECTED_VS_ACTUAL, List.of(customResponse));
			} else if (testCaseName.contains("UinNotAllocated")) {

				OutputValidationDto customResponse = IdRepoUtil.assertDraftUinNotAllocated(response);

				ouputValid = new HashMap<>();
				ouputValid.put(GlobalConstants.EXPECTED_VS_ACTUAL, List.of(customResponse));
			} else {
				ouputValid = OutputValidationUtil.doJsonOutputValidation(response.asString(),
						getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()), testCaseDTO,
						response.getStatusCode());
			}

			Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));
			if (!OutputValidationUtil.publishOutputResult(ouputValid))
				throw new AdminTestException("Failed at output validation");
		}
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
}
