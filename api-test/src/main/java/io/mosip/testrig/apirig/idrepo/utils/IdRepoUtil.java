package io.mosip.testrig.apirig.idrepo.utils;

import java.util.ArrayList;
import java.util.List;
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

		JSONArray dobArray = null;
		JSONArray emailArray = null;
		String dobActuator = getValueFromAuthActuator("json-property", "dob");
		String emailActuator = getValueFromAuthActuator("json-property", "emailId");
		if (dobActuator != null && !dobActuator.isBlank()) {
			dobArray = new JSONArray(dobActuator);
		}
		if (emailActuator != null && !emailActuator.isBlank()) {
			emailArray = new JSONArray(emailActuator);
		}
		String dob = (dobArray != null && !dobArray.isEmpty()) ? dobArray.getString(0) : "dateOfBirth";
		String email = (emailArray != null && !emailArray.isEmpty()) ? emailArray.getString(0) : "email";

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
		runCleanup("keymgr", () -> DBManager.executeDBQueries(IdRepoConfigManager.getKMDbUrl(),
				IdRepoConfigManager.getKMDbUser(), IdRepoConfigManager.getKMDbPass(),
				IdRepoConfigManager.getKMDbSchema(),
				getGlobalResourcePath() + "/" + "config/keyManagerCertDataDeleteQueries.txt"));
		runCleanup("ida", () -> DBManager.executeDBQueries(IdRepoConfigManager.getIdaDbUrl(),
				IdRepoConfigManager.getIdaDbUser(), IdRepoConfigManager.getPMSDbPass(),
				IdRepoConfigManager.getIdaDbSchema(),
				getGlobalResourcePath() + "/" + "config/idaCertDataDeleteQueries.txt"));
		runCleanup("master", () -> DBManager.executeDBQueries(IdRepoConfigManager.getMASTERDbUrl(),
				IdRepoConfigManager.getMasterDbUser(), IdRepoConfigManager.getMasterDbPass(),
				IdRepoConfigManager.getMasterDbSchema(),
				getGlobalResourcePath() + "/" + "config/masterDataCertDataDeleteQueries.txt"));
		runCleanup("idrepo", () -> DBManager.executeDBQueries(IdRepoConfigManager.getIdRepoDbUrl(),
				IdRepoConfigManager.getIdRepoDbUser(), IdRepoConfigManager.getPMSDbPass(), "idrepo",
				getGlobalResourcePath() + "/" + "config/idrepoDeleteQueries.txt"));
	}

	private static void runCleanup(String schema, Runnable cleanup) {
		try {
			cleanup.run();
		} catch (Throwable t) {
			logger.error("DB cleanup skipped for " + schema + ": " + t.getClass().getSimpleName() + " - "
					+ t.getMessage());
		}
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