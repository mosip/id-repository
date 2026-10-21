package io.mosip.testrig.apirig.idrepo.testscripts;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
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
 * Variant of {@link SimplePost} that also caches values from the response for later YAML cases,
 * for example CreateVid ({@code idKeyName=VID}), DeactivateVid, ReactivateVIDsForUIN,
 * RetrieveIdentityPost and CreateDraftVID.
 * <p>
 * The suite parameter {@code idKeyName} lists the response fields to capture (comma-separated).
 * Each value is cached under {@code <testCaseName>_<field>} and can be read later as
 * {@code $ID:<testCaseName>_<field>$}.
 */
public class SimplePostForAutoGenId extends IdRepoUtil implements ITest {
    private static final Logger logger = Logger.getLogger(SimplePostForAutoGenId.class);
    /** Name of the case currently running; becomes the TestNG method name in reports. */
    protected String testCaseName = "";
    /** Suite parameter {@code idKeyName}: response fields to cache. */
    public String idKeyName = null;
    /** Response of the last call. */
    public Response response = null;
    /** True when the suite {@code <test>} declares a {@code sendEsignetToken} parameter. */
    public boolean sendEsignetToken = false;
    /** Never set from YAML in this script, so always {@code false}. */
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
     * {@code idKeyName} and {@code sendEsignetToken} parameters.
     *
     * @param context TestNG context holding the {@code <test>} parameters
     * @return one {@link TestCaseDTO} per YAML entry
     */
    @DataProvider(name = "testcaselist")
    public Object[] getTestCaseList(ITestContext context) {
        String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
        sendEsignetToken = context.getCurrentXmlTest().getLocalParameters().containsKey("sendEsignetToken");
        idKeyName = context.getCurrentXmlTest().getLocalParameters().get("idKeyName");
        logger.info("Started executing yml: " + ymlFile);
        return getYmlTestData(ymlFile);
    }

    /**
     * Runs one POST case and caches the {@link #idKeyName} fields from the response.
     * <p>
     * Uses the same skip rules as the other scripts (execution gate, health check, VID support).
     * With YAML {@code templateFields} the call runs once per configured language; for
     * {@code dynamic} cases {@code response.id} is also stored in {@code idField}. Otherwise it
     * runs once and is validated in status-code mode ({@code _StatusCode}) or output-template mode.
     *
     * @param testCaseDTO the YAML case
     * @throws AuthenticationTestException on token generation failure
     * @throws AdminTestException          when validation fails
     * @throws NoSuchAlgorithmException    declared by the commons REST helper
     * @throws SecurityXSSException        when the XSS header check is enabled and fails
     */
    @Test(dataProvider = "testcaselist")
    public void test(TestCaseDTO testCaseDTO)
            throws AuthenticationTestException, AdminTestException, NoSuchAlgorithmException, SecurityXSSException {
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

        String[] templateFields = testCaseDTO.getTemplateFields();
        String inputJson = "";

        inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());

//			String outputJson = getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate());

        if (testCaseDTO.getTemplateFields() != null && templateFields.length > 0) {
            ArrayList<JSONObject> inputtestCases = AdminTestUtil.getInputTestCase(testCaseDTO);
            ArrayList<JSONObject> outputtestcase = AdminTestUtil.getOutputTestCase(testCaseDTO);
            for (int i = 0; i < languageList.size(); i++) {
                response = postWithBodyAndCookieForAutoGeneratedId(ApplnURI + testCaseDTO.getEndPoint(),
                        getJsonFromTemplate(inputtestCases.get(i).toString(), testCaseDTO.getInputTemplate()),
                        COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), idKeyName);

                Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
                        response.asString(),
                        getJsonFromTemplate(outputtestcase.get(i).toString(), testCaseDTO.getOutputTemplate()),
                        testCaseDTO, response.getStatusCode());
                if (testCaseDTO.getTestCaseName().toLowerCase().contains("dynamic")) {
                    JSONObject json = new JSONObject(response.asString());
                    idField = json.getJSONObject("response").get("id").toString();
                }
                Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

                if (!OutputValidationUtil.publishOutputResult(ouputValid))
                    throw new AdminTestException("Failed at output validation");
            }
        } else {
            response = postWithBodyAndCookieForAutoGeneratedId(ApplnURI + testCaseDTO.getEndPoint(), inputJson,
                    auditLogCheck, COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), idKeyName,
                    sendEsignetToken);

            Map<String, List<OutputValidationDto>> ouputValid = null;
            if (testCaseName.contains("_StatusCode")) {

                OutputValidationDto customResponse = customStatusCodeResponse(String.valueOf(response.getStatusCode()),
                        testCaseDTO.getOutput());

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
