package io.mosip.testrig.apirig.idrepo.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.testng.SkipException;

import io.mosip.testrig.apirig.dbaccess.DBManager;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;

public class IdRepoUtil extends AdminTestUtil {

	private static final Logger logger = Logger.getLogger(IdRepoUtil.class);
	public static String genRidExt = "23456" + generateRandomNumberString(10);
	public static List<String> testCasesInRunScope = new ArrayList<>();
	
	public static void setLogLevel() {
		if (IdRepoConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}
	
	public static String isTestCaseValidForExecution(TestCaseDTO testCaseDTO) {
		String testCaseName = testCaseDTO.getTestCaseName();
		currentTestCaseName = testCaseName;

		int indexof = testCaseName.indexOf("_");
		String modifiedTestCaseName = testCaseName.substring(indexof + 1);

		addTestCaseDetailsToMap(modifiedTestCaseName, testCaseDTO.getUniqueIdentifier());
		
		if (!testCasesInRunScope.isEmpty()
				&& testCasesInRunScope.contains(testCaseDTO.getUniqueIdentifier()) == false) {
			throw new SkipException(GlobalConstants.NOT_IN_RUN_SCOPE_MESSAGE);
		}
		
		// Handle extra workflow dependencies
		if (testCaseDTO != null && testCaseDTO.getAdditionalDependencies() != null
				&& AdminTestUtil.generateDependency == true) {
			addAdditionalDependencies(testCaseDTO);
		}

		if (SkipTestCaseHandler.isTestCaseInSkippedList(testCaseName)) {
			throw new SkipException(GlobalConstants.KNOWN_ISSUES);
		}

		if (testCaseDTO.getRequiredSchemaFields() != null && testCaseDTO.getRequiredSchemaFields().length > 0) {
			for (String field : testCaseDTO.getRequiredSchemaFields()) {
				String trimmed = field.trim();
				if (globalRequiredFields == null || !isElementPresent(globalRequiredFields, trimmed)) {
					throw new SkipException(
							"Schema field '" + trimmed + "' not present in current IdSchema — test not applicable");
				}
			}
		}

		JSONArray dobArray = new JSONArray(getValueFromAuthActuator("json-property", "dob"));
		String dob = dobArray.getString(0);
		JSONArray emailArray = new JSONArray(getValueFromAuthActuator("json-property", "emailId"));
		String email = emailArray.getString(0);

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
	
	public static void dbCleanUp() {
		DBManager.executeDBQueries(IdRepoConfigManager.getKMDbUrl(), IdRepoConfigManager.getKMDbUser(),
				IdRepoConfigManager.getKMDbPass(), IdRepoConfigManager.getKMDbSchema(),
				getGlobalResourcePath() + "/" + "config/keyManagerCertDataDeleteQueries.txt");
		DBManager.executeDBQueries(IdRepoConfigManager.getIdaDbUrl(), IdRepoConfigManager.getIdaDbUser(),
				IdRepoConfigManager.getPMSDbPass(), IdRepoConfigManager.getIdaDbSchema(),
				getGlobalResourcePath() + "/" + "config/idaCertDataDeleteQueries.txt");
		DBManager.executeDBQueries(IdRepoConfigManager.getMASTERDbUrl(), IdRepoConfigManager.getMasterDbUser(),
				IdRepoConfigManager.getMasterDbPass(), IdRepoConfigManager.getMasterDbSchema(),
				getGlobalResourcePath() + "/" + "config/masterDataCertDataDeleteQueries.txt");

		DBManager.executeDBQueries(IdRepoConfigManager.getIdRepoDbUrl(), IdRepoConfigManager.getIdRepoDbUser(),
				IdRepoConfigManager.getPMSDbPass(), "idrepo",
				getGlobalResourcePath() + "/" + "config/idrepoCertDataDeleteQueries.txt");
	}
	
	public String applyAddIdentityOverrides(String inputJson, String testCaseName) {
	    Map<String, String> replacements = new LinkedHashMap<>();

	    if (testCaseName.contains("_withInvalidEmail") || testCaseName.contains("_invalid_Email")) {
	        replacements.put("$EMAILVALUE$", "@#$DDFFGG");
	    }
	    if (testCaseName.contains("Empty_Email")) {
	        replacements.put("$EMAILVALUE$", " ");
	    }
	    if (testCaseName.contains("SpaceVal_Email")) {
	        replacements.put("$EMAILVALUE$", "  ");
	    }
	    if (testCaseName.contains("_withInvalidPhone") || testCaseName.contains("_invalid_Phone")) {
	        replacements.put("$PHONENUMBERFORIDENTITY$", "@%+++456789345678");
	    }
	    if (testCaseName.contains("_withEmptyPhone")) {
	        replacements.put("$PHONENUMBERFORIDENTITY$", " ");
	    }

	    for (Map.Entry<String, String> entry : replacements.entrySet()) {
	        inputJson = replaceKeywordWithValue(inputJson, entry.getKey(), entry.getValue());
	    }

	    return inputJson;
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