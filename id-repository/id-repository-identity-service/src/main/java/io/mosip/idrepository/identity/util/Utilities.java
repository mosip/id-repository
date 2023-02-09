package io.mosip.idrepository.identity.util;

import java.io.IOException;
import java.util.Map;

import org.jose4j.json.internal.json_simple.JSONObject;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.khazana.config.LoggerConfiguration;
import io.mosip.kernel.core.logger.spi.Logger;
import lombok.Data;

/**
 * The Class Utilities.
 *
 * @author Neha Farheen
 */
@Component

/**
 * Instantiates a new utilities.
 */
@Data
public class Utilities {
	private final Logger logger = LoggerConfiguration.logConfig(Utilities.class);
	
	private String mappingJsonString = null;
    
	@Value("${mosip.idrepo.identityjson.file}")
	private String identityJson;
	
	@Value("${mosip.idrepo.mosip-config-url}")
	private String configServerFileStorageURL;
    
    @Autowired
	private ObjectMapper objMapper;
    
    private JSONObject mappingJsonObject = null;
    
    public static String getJson(String configServerFileStorageURL, String uri) {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(configServerFileStorageURL + uri, String.class);
	}

	public String getMappingJsonValue(String key, String identity) throws IOException, JSONException {
		JSONObject jsonObject = getIdentityMappingJson(identity);
		Object obj = jsonObject.get(key);
		if (obj instanceof Map) {
			Map<?,?> map = (Map<?,?>) obj;
			return map.get("value") != null ? map.get("value").toString() : null;
		}
		return jsonObject.get(key) != null ? jsonObject.get(key).toString() : null;
	}

	public JSONObject getIdentityMappingJson(String identity) throws IOException {
		logger.debug("Utilities::getRegistrationProcessorMappingJson()::entry");
		if (mappingJsonObject == null) {
			String mappingJsonString = Utilities.getJson(configServerFileStorageURL, identityJson);
			mappingJsonObject = objMapper.readValue(mappingJsonString, JSONObject.class);
		}
		logger.debug("Utilities::getRegistrationProcessorMappingJson()::exit");
		return JsonUtil.getJSONObject(mappingJsonObject, identity);

	}

}
