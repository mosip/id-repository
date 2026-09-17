package io.mosip.testrig.apirig.idrepo.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.testng.SkipException;

import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;

public class IdRepoUtil extends AdminTestUtil {

	private static final Logger logger = Logger.getLogger(IdRepoUtil.class);
	public static String genRidExt = "23456" + generateRandomNumberString(10);
	
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}
	
	public static String isTestCaseValidForExecution(TestCaseDTO testCaseDTO) {
		String testCaseName = testCaseDTO.getTestCaseName();

		int indexof = testCaseName.indexOf("_");
		String modifiedTestCaseName = testCaseName.substring(indexof + 1);

		addTestCaseDetailsToMap(modifiedTestCaseName, testCaseDTO.getUniqueIdentifier());

		if (SkipTestCaseHandler.isTestCaseInSkippedList(testCaseName)) {
			throw new SkipException(GlobalConstants.KNOWN_ISSUES);
		}

		JSONArray dobArray = new JSONArray(getValueFromAuthActuator("json-property", "dob"));
		String dob = dobArray.getString(0);
		JSONArray emailArray = new JSONArray(getValueFromAuthActuator("json-property", "emailId"));
		String email = emailArray.getString(0);
//		JSONArray phoneArray = new JSONArray(getValueFromAuthActuator("json-property", "phone_number"));

		if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("DOB")
				&& (!isElementPresent(globalRequiredFields, dob))) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("_handle")
				&& foundHandlesInIdSchema == false) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}


		if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("Email")
				&& (!isElementPresent(globalRequiredFields, email))) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		else if (testCaseName.startsWith("IdRepository_") && testCaseName.contains("Invalid_BioVal")
				&& (ConfigManager.isInServiceNotDeployedList(GlobalConstants.ADMIN))) {
			throw new SkipException(GlobalConstants.FEATURE_NOT_SUPPORTED_MESSAGE);
		}

		return testCaseName;
	}
	
	public static String inputStringKeyWordHandeler(String jsonString, String testCaseName) {
		if (jsonString == null) {
			logger.info(" Request Json String is :" + jsonString);
			return jsonString;
		}
		
		if (jsonString.contains("$RIDEXT$"))
			jsonString = replaceKeywordWithValue(jsonString, "$RIDEXT$", genRidExt);
		
		return jsonString;
	}

	
}