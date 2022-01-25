package io.mosip.credentialstore.util;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.dto.Issuer;
import io.mosip.credentialstore.dto.Type;
import io.mosip.credentialstore.exception.VerCredException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class Utilities {

	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String DESCRIPTION = "description";
	private static final String ISSUERS = "issuers";
	private static final String CODE = "code";
	private static final String UTILITIES = "Utilities";
	private static final String GETTYPES = "getTypes";

	private static final Logger LOGGER = IdRepoLogger.getLogger(Utilities.class);

	@Autowired
	private RestTemplate restTemplate;
	
	public List<Type> getTypes(String configServerFileStorageURL, String uri) {
		List<Type> typeList = new ArrayList<>();
		JSONObject credentialTypes;
		try {
			String types = restTemplate.getForObject(configServerFileStorageURL + uri, String.class);
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
			LOGGER.error(IdRepoSecurityManager.getUser(), UTILITIES, GETTYPES,
					"error while getting types" + ExceptionUtils.getStackTrace(e));
		}
		return typeList;
	}
	
	public String generateId() {
		return UUID.randomUUID().toString();
	}
	
	public String generatePin() {
		return  RandomStringUtils.randomAlphabetic(5);
	}

	public JSONObject getVCContext(String configServerFileStorageURL, String uri) {
		try {
			String vcContextStr = restTemplate.getForObject(configServerFileStorageURL + uri, String.class);
			JSONObject vcContext = JsonUtil.objectMapperReadValue(vcContextStr, JSONObject.class);
			return vcContext;
		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), UTILITIES, "VCContext",
					"error while getting VC Context Json." + ExceptionUtils.getStackTrace(e));
			throw new VerCredException(CredentialServiceErrorCodes.VC_CONTEXT_FILE_NOT_FOUND.getErrorCode(),
					CredentialServiceErrorCodes.VC_CONTEXT_FILE_NOT_FOUND.getErrorMessage());
		}
	}

	public JsonDocument getVCContextJson(String configServerFileStorageURL, String uri) {
		try {
			String vcContextJson = restTemplate.getForObject(configServerFileStorageURL + uri, String.class);
			JsonDocument jsonDocument = JsonDocument.of(new StringReader(vcContextJson));
			return jsonDocument;
		} catch (JsonLdError e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), UTILITIES, "VCContextJson",
					"error while getting VC Context Json Document." + ExceptionUtils.getStackTrace(e));
			throw new VerCredException(CredentialServiceErrorCodes.VC_CONTEXT_FILE_NOT_FOUND.getErrorCode(),
					CredentialServiceErrorCodes.VC_CONTEXT_FILE_NOT_FOUND.getErrorMessage());
		}
	}
}
