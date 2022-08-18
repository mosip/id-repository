package io.mosip.idrepository.identity.provider;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.util.EnvUtil;

/**
 * @author Manoj SP
 * @since 1.2.0.2
 *
 */
@Component
@RefreshScope
public class IdentityUpdateTrackerPolicyProvider {
	
	@Autowired
	private ObjectMapper mapper;
	
	private static Map<String, Integer> updateCount = Map.of();
	
	@PostConstruct
	public void loadUpdateCountPolicies() throws IOException {
		JsonNode policyJson = mapper.readValue(new URL(EnvUtil.getIdentityMappingJsonUrl()), JsonNode.class);
		String updatePolicyJson = policyJson.get("attributeUpdateCountLimit").toString();
		updateCount = mapper.readValue(updatePolicyJson, new TypeReference<Map<String, Integer>>() {
		});
	}
	
	public static Map<String, Integer> getUpdateCountLimitMap() {
		return updateCount;
	}
	
	public static Integer getMaxUpdateCountLimit(String attribute) {
		return updateCount.get(attribute);
	}
	
}
