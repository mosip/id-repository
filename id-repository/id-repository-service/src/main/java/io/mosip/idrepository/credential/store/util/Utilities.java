package io.mosip.idrepository.credential.store.util;


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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.idrepository.credential.store.dto.Issuer;
import io.mosip.idrepository.credential.store.dto.Type;
import io.mosip.idrepository.credential.store.exception.VerCredException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Config-server and JSON-LD helpers for credential issuance.
 * <p>
 * Loads credential type definitions and verifiable-credential context documents
 * from the Spring Config Server file storage, and provides UUID generation for
 * credential identifiers.
 * </p>
 */
@Component("credentialStoreUtilities")
public class Utilities {

	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String DESCRIPTION = "description";
	private static final String ISSUERS = "issuers";
	private static final String CODE = "code";
	private static final String UTILITIES = "Utilities";
	private static final String GETTYPES = "getTypes";

	private static final Logger LOGGER = IdRepoLogger.getLogger(Utilities.class);

	/**
	 * Plain {@link RestTemplate} for config-server HTTP fetches (no auth adapter).
	 */
	@Autowired
	@Qualifier("plainRestTemplate")
	private RestTemplate restTemplate;

	/**
	 * Fetches and parses the credential types JSON from config server storage.
	 * <p>
	 * Expects a top-level {@code types} array; each entry may include nested {@code issuers}.
	 * Returns an empty list when the fetch or parse fails (errors are logged).
	 * </p>
	 *
	 * @param configServerFileStorageURL base URL of config server file storage
	 * @param uri                        relative path to the credential types file
	 * @return list of {@link Type} definitions, possibly empty on error
	 */
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

	/**
	 * Generates a random UUID string for credential or transaction identifiers.
	 *
	 * @return string form of {@link UUID#randomUUID()}
	 */
	public String generateId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Loads a verifiable-credential JSON-LD context as a {@link JSONObject}.
	 *
	 * @param configServerFileStorageURL base URL of config server file storage
	 * @param uri                        relative path to the VC context file
	 * @return parsed context object
	 * @throws VerCredException when the file cannot be fetched or parsed
	 */
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

	/**
	 * Loads a verifiable-credential JSON-LD context as a {@link JsonDocument}.
	 * <p>
	 * Used by JSON-LD credential formatters that require a parsed document model.
	 * </p>
	 *
	 * @param configServerFileStorageURL base URL of config server file storage
	 * @param uri                        relative path to the VC context file
	 * @return JSON-LD document for context expansion
	 * @throws VerCredException when the file cannot be fetched or is invalid JSON-LD
	 */
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
