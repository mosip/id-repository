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
	
	public static void init() {
		Logger configManagerLogger = Logger.getLogger(ConfigManager.class);
		configManagerLogger.setLevel(Level.WARN);
		
		Map<String, Object> moduleSpecificPropertiesMap = new HashMap<>();
		// Load scope specific properties
		try {
			String path = MosipTestRunner.getGlobalResourcePath() + "/config/Idrepo.properties";
			Properties props = getproperties(path);
			// Convert Properties to Map and add to moduleSpecificPropertiesMap
			for (String key : props.stringPropertyNames()) {
				moduleSpecificPropertiesMap.put(key, props.getProperty(key));
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		// Add module specific properties as well.
		init(moduleSpecificPropertiesMap);
	}

	
	 
}