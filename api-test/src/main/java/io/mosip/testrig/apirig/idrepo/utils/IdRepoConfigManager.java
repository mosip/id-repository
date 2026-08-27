package io.mosip.testrig.apirig.idrepo.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import io.mosip.testrig.apirig.idrepo.testrunner.MosipTestRunner;
import io.mosip.testrig.apirig.utils.ConfigManager;

public class IdRepoConfigManager extends ConfigManager{
	private static final Logger LOGGER = Logger.getLogger(IdRepoConfigManager.class);

	private static final String LOCAL_PROPERTIES = "Idrepo-local.properties";
	private static final String ENV_PROPERTIES = "Idrepo.properties";

	public static void init() {
		Logger configManagerLogger = Logger.getLogger(ConfigManager.class);
		configManagerLogger.setLevel(Level.WARN);
		
		Map<String, Object> moduleSpecificPropertiesMap = new HashMap<>();
		// Auto-select: localhost / 127.0.0.1 in -Denv.endpoint → Idrepo-local.properties,
		// otherwise Idrepo.properties. Optional override: -Didrepo.propertiesFile=<name>.
		// Env vars still win via ConfigManager.
		try {
			String propertiesFile = resolvePropertiesFile();
			String path = MosipTestRunner.getGlobalResourcePath() + "/config/" + propertiesFile;
			LOGGER.info("Loading idrepo config from: " + path);
			Properties props = getproperties(path);
			for (String key : props.stringPropertyNames()) {
				moduleSpecificPropertiesMap.put(key, props.getProperty(key));
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		init(moduleSpecificPropertiesMap);
	}

	/**
	 * Resolves which properties file to load.
	 * <ul>
	 *   <li>Explicit {@code -Didrepo.propertiesFile=...} wins if set</li>
	 *   <li>Else if {@code -Denv.endpoint} contains localhost / 127.0.0.1 → {@code Idrepo-local.properties}</li>
	 *   <li>Else → {@code Idrepo.properties} (remote env)</li>
	 * </ul>
	 */
	static String resolvePropertiesFile() {
		String override = System.getProperty("idrepo.propertiesFile");
		if (override != null && !override.isBlank()) {
			LOGGER.info("Using explicit idrepo.propertiesFile=" + override);
			return override.trim();
		}
		if (isLocalEndpoint()) {
			LOGGER.info("env.endpoint is localhost → loading " + LOCAL_PROPERTIES);
			return LOCAL_PROPERTIES;
		}
		LOGGER.info("env.endpoint is remote → loading " + ENV_PROPERTIES);
		return ENV_PROPERTIES;
	}

	static boolean isLocalEndpoint() {
		String endpoint = System.getProperty("env.endpoint", "");
		if (endpoint == null || endpoint.isBlank()) {
			return false;
		}
		String lower = endpoint.toLowerCase();
		return lower.contains("localhost") || lower.contains("127.0.0.1");
	}
}
