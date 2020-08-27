package io.mosip.credentialstore.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import io.mosip.credentialstore.dto.Issuer;
import io.mosip.credentialstore.dto.Type;

@Component
public class Utilities {

	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String DESCRIPTION = "description";
	private static final String ISSUERS = "issuers";
	private static final String CODE = "code";

	
	public List<Type> getTypes(String configServerFileStorageURL, String uri) {
		List<Type> typeList = new ArrayList<>();
		RestTemplate restTemplate = new RestTemplate();
		String types = restTemplate.getForObject(configServerFileStorageURL + uri, String.class);

		JSONObject credentialTypes;
		try {
			credentialTypes = JsonUtil.objectMapperReadValue(types, JSONObject.class);
			JSONArray credentialTypeArray = JsonUtil.getJSONArray(credentialTypes, "types");
			for (Object jsonObject : credentialTypeArray) {
				Type type = new Type();
				JSONObject json = new JSONObject((Map) jsonObject);
				type.setId(JsonUtil.getJSONValue(json, ID));
				type.setName(JsonUtil.getJSONValue(json, NAME));
				type.setDescription(JsonUtil.getJSONValue(json, DESCRIPTION));
				JSONArray issuersArray = JsonUtil.getJSONArray(json, ISSUERS);
				List<Issuer> issuerList = new ArrayList<>();
				for (Object issuerJsonObject : issuersArray) {
					Issuer issuer = new Issuer();
					JSONObject isserJson = new JSONObject((Map) issuerJsonObject);
					issuer.setCode(JsonUtil.getJSONValue(isserJson, CODE));
					issuer.setName(JsonUtil.getJSONValue(isserJson, NAME));
					issuerList.add(issuer);
				}
				type.setIssuers(issuerList);
				typeList.add(type);
			}
		} catch (IOException e) {
			// log error
		}


		return typeList;
	}
	public String generateId() {
		return UUID.randomUUID().toString();
	}
	
	public String generatePin() {
	return  RandomStringUtils.randomAlphabetic(5);
	}

}
