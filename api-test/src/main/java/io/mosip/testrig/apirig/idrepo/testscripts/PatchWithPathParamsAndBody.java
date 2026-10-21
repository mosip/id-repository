package io.mosip.testrig.apirig.idrepo.testscripts;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
 * Generic TestNG script for {@code PATCH} endpoints that take both path parameters and a JSON body:
 * VID status update ({@code UpdateVidStatus.yml}, {@code pathParams=VID}) and the draft UIN-data
 * update ({@code UpdateDraftUinData.yml}, {@code pathParams=registrationId}).
 * <p>
 * The suite parameter {@code pathParams} lists the rendered input fields to move into the URL
 * (comma-separated); the remaining fields form the body. {@code $1STLANG$}, {@code $2STLANG$} and
 * {@code $3STLANG$} become the first three configured languages.
 */
public class PatchWithPathParamsAndBody extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(PatchWithPathParamsAndBody.class);
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
	 * Runs one PATCH case.
	 * <p>
	 * Uses the same skip rules as the other scripts (execution gate, health check, VID support).
	 * Adjusts the HBS templates to the configured languages ({@code AdminTestUtil.filterHbs}),
	 * renders the input with language and registration-id placeholders resolved, sends the PATCH
	 * and validates the response against the YAML output template.
	 *
	 * @param testCaseDTO the YAML case
	 * @throws AdminTestException   when output validation fails
	 * @throws SecurityXSSException when the XSS header check is enabled and fails
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AdminTestException, SecurityXSSException {
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

		testCaseDTO = AdminTestUtil.filterHbs(testCaseDTO);
		String inputJson = filterInputHbs(testCaseDTO);
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
	 * Renders the YAML input through its template and replaces {@code $1STLANG$}, {@code $2STLANG$}
	 * and {@code $3STLANG$} with the first, second and third configured languages.
	 *
	 * @param testCaseDTO the YAML case
	 * @return rendered request JSON
	 * @throws IndexOutOfBoundsException when a placeholder refers to a language that is not configured
	 */
	private String filterInputHbs(TestCaseDTO testCaseDTO) {
		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());

		if (inputJson.contains(GlobalConstants.$1STLANG$))
			inputJson = inputJson.replace(GlobalConstants.$1STLANG$, BaseTestCase.languageList.get(0));
		if (inputJson.contains(GlobalConstants.$2STLANG$))
			inputJson = inputJson.replace(GlobalConstants.$2STLANG$, BaseTestCase.languageList.get(1));
		if (inputJson.contains(GlobalConstants.$3STLANG$))
			inputJson = inputJson.replace(GlobalConstants.$3STLANG$, BaseTestCase.languageList.get(2));

		return inputJson;
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
