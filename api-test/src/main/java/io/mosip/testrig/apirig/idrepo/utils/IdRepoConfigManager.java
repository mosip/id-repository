package io.mosip.testrig.apirig.idrepo.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import io.mosip.testrig.apirig.idrepo.testrunner.MosipTestRunner;
import io.mosip.testrig.apirig.utils.ConfigManager;

/**
 * ID Repository flavour of the apitest-commons {@link ConfigManager}.
 * <p>
 * Adds the module-specific properties file to the common configuration. All getters (DB URLs,
 * credentials, {@code IsDebugEnabled()}, {@code getproperty(..)}) are inherited from
 * {@link ConfigManager}.
 */
public class IdRepoConfigManager extends ConfigManager{
	private static final Logger LOGGER = Logger.getLogger(IdRepoConfigManager.class);
	
	/**
	 * Loads the idrepo properties file and passes its entries to {@link ConfigManager#init(Map)}.
	 * <p>
	 * The file is read from {@code <globalResourcePath>/config/}. Its name comes from the system
	 * property {@code idrepo.propertiesFile} (default {@code Idrepo.properties}); when the default
	 * file is missing, {@code IDRepo.properties} is tried. A load failure is logged and the common
	 * configuration is still initialised, without module overrides.
	 * <p>
	 * Call after {@code AdminTestUtil.init()} and the resource extraction step, so the config
	 * folder exists.
	 */
	public static void init() {
		Logger configManagerLogger = Logger.getLogger(ConfigManager.class);
		configManagerLogger.setLevel(Level.WARN);
		
		Map<String, Object> moduleSpecificPropertiesMap = new HashMap<>();
		// Load scope specific properties (default Idrepo.properties; override with -Didrepo.propertiesFile=...)
		try {
			String requested = System.getProperty("idrepo.propertiesFile", "Idrepo.properties");
			if (requested == null || requested.isBlank()) {
				requested = "Idrepo.properties";
			}
			String resourceRoot = MosipTestRunner.getGlobalResourcePath() + "/config/";
			File propsFile = new File(resourceRoot + requested);
			if (!propsFile.isFile() && "Idrepo.properties".equals(requested)) {
				propsFile = new File(resourceRoot + "IDRepo.properties");
			}
			LOGGER.info("Loading idrepo properties from: " + propsFile.getAbsolutePath());
			Properties props = getproperties(propsFile.getAbsolutePath());
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