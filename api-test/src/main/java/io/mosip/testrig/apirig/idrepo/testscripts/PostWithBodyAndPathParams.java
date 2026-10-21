package io.mosip.testrig.apirig.idrepo.testscripts;

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

import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoConfigManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoUtil;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

/**
 * Generic TestNG script for {@code POST} endpoints that take both path parameters and a JSON body,
 * for example CreateDraftV2 ({@code POST /idrepository/v1/identity/draft/v2/create/{registrationId}}).
 * <p>
 * The suite parameter {@code pathParams} lists the rendered input fields to move into the URL; the
 * remaining fields form the body. Templates render every value as a string, so
 * {@code "generateUin": "true"} and {@code "generateUin": "false"} are turned back into JSON booleans
 * before sending.
 */
public class PostWithBodyAndPathParams extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(PostWithBodyAndPathParams.class);
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
	 * Runs one POST case.
	 * <p>
	 * Skips when {@link IdRepoUtil#isTestCaseValidForExecution} rejects the case or the environment
	 * failed its health check (no VID check). Renders the input, resolves registration-id
	 * placeholders, restores the {@code generateUin} boolean, POSTs and validates the response
	 * against the YAML output template.
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

		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());
		inputJson = inputStringKeyWordHandeler(inputJson, testCaseName);
		// generateUin is a real boolean on the wire; unquote it back from the template's string form
		inputJson = inputJson.replace("\"generateUin\": \"true\"", "\"generateUin\": true");
		inputJson = inputJson.replace("\"generateUin\": \"false\"", "\"generateUin\": false");

		response = postWithPathParamsBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), pathParams);

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()),
				testCaseDTO, response.getStatusCode());
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

		if (!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AdminTestException("Failed at output validation");
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
