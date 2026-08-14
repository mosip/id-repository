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

// Generic POST script for endpoints with both a path param and a JSON body (e.g. createDraftV2).
// "pathParams" (comma separated, from suite XML) names which YAML input fields become path
// params; the rest of the fields are sent as the request body.
public class PostWithBodyAndPathParams extends IdRepoUtil implements ITest {
	private static final Logger logger = Logger.getLogger(PostWithBodyAndPathParams.class);
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

		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());
		inputJson = inputStringKeyWordHandeler(inputJson, testCaseName);
		// generateUin is a real boolean on the wire; the template quotes it (so "$REMOVE$" stays
		// valid JSON when the field is omitted), so unquote real true/false values back to JSON
		// booleans here. $REMOVE$ itself is untouched and gets stripped by inputJsonKeyWordHandeler
		// inside postWithPathParamsBodyAndCookie below.
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

	/** Sets current test name to result */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		result.setAttribute("TestCaseName", testCaseName);
	}
}
