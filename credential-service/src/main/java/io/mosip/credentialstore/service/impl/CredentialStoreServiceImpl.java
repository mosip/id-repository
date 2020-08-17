package io.mosip.credentialstore.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.credentialstore.constants.CredentialFormatter;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.CredentialType;
import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.CredentialServiceResponseDto;
import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.ErrorDTO;
import io.mosip.credentialstore.dto.IdResponseDto;
import io.mosip.credentialstore.dto.JsonValue;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;
import io.mosip.credentialstore.dto.Type;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.credentialstore.util.CredentialFormatterMapperUtil;
import io.mosip.credentialstore.util.DataShareUtil;
import io.mosip.credentialstore.util.IdrepositaryUtil;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.PolicyUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.credentialstore.util.WebSubUtil;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class CredentialStoreServiceImpl.
 * 
 * @author Sowmya
 */
@Component
public class CredentialStoreServiceImpl implements CredentialStoreService {

	/** The policy util. */
	@Autowired
	private PolicyUtil policyUtil;

	/** The idrepositary util. */
	@Autowired
	private IdrepositaryUtil idrepositaryUtil;

	/** The primary lang. */
	@Value("${mosip.primary-language}")
	private String primaryLang;

	/** The secondary lang. */
	@Value("${mosip.secondary-language}")
	private String secondaryLang;

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	/** The credential formatter mapper util. */
	@Autowired
	private CredentialFormatterMapperUtil credentialFormatterMapperUtil;

	/** The id auth provider. */
	@Autowired(required = false)
	@Qualifier("idauth")
	CredentialProvider idAuthProvider;

	/** The default provider. */
	@Autowired(required = false)
	@Qualifier("default")
	CredentialProvider defaultProvider;

	/** The pin based provider. */
	@Autowired(required = false)
	@Qualifier("pin")
	CredentialProvider pinBasedProvider;

	/** The data share util. */
	@Autowired
	private DataShareUtil dataShareUtil;

	/** The web sub util. */
	@Autowired
	private WebSubUtil webSubUtil;

	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${credential.service.credentialtype.file}")
	private String credentialTypefile;

	@Autowired
	Utilities utilities;

	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_ID. */
	private static final String CREDENTIAL_SERVICE_SERVICE_ID = "mosip.credential.service.service.id";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_VERSION. */
	private static final String CREDENTIAL_SERVICE_SERVICE_VERSION = "mosip.credential.service.service.version";

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credentialstore.service.CredentialStoreService#
	 * createCredentialIssuance(io.mosip.credentialstore.dto.
	 * CredentialServiceRequestDto)
	 */
	public CredentialServiceResponseDto createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto) {
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialServiceResponseDto credentialIssueResponseDto = new CredentialServiceResponseDto();
		CredentialProvider credentialProvider;
		String status = null;
		try{
			PolicyDetailResponseDto policyDetailResponseDto = policyUtil.getPolicyDetail(credentialServiceRequestDto.getCredentialType(), credentialServiceRequestDto.getIssuer());
		

		if (policyDetailResponseDto != null) {

				IdResponseDto idResponseDto = idrepositaryUtil.getData(credentialServiceRequestDto.getId(),
					credentialServiceRequestDto.getFormatter());

				Map<String, Object> sharableAttributeMap = setSharableAttributeValues(idResponseDto,
					credentialServiceRequestDto, policyDetailResponseDto);

				credentialProvider = getProvider(credentialServiceRequestDto.getCredentialType());

				byte[] credentialData = credentialProvider.getFormattedCredentialData(policyDetailResponseDto,
						credentialServiceRequestDto,
						sharableAttributeMap);

				DataShare dataShare = dataShareUtil.getDataShare(credentialData, policyDetailResponseDto.getId(),
						credentialServiceRequestDto.getIssuer());

				webSubUtil.publishSuccess(dataShare);
				status = "DONE";

			
		} else {
				ErrorDTO error = new ErrorDTO();
				error.setErrorCode(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode());
				error.setMessage(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage());
				errorList.add(error);
				status = "FAILED";

		}
		
		} catch (ApiNotAccessibleException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
			errorList.add(error);
			status = "FAILED";
		} catch (IdRepoException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			status = "FAILED";
		} catch (CredentialFormatterException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			status = "FAILED";
		} catch (IOException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			status = "FAILED";

		} catch (Exception e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			status = "FAILED";

		}finally {

			credentialIssueResponseDto.setId(CREDENTIAL_SERVICE_SERVICE_ID);
			credentialIssueResponseDto
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_SERVICE_SERVICE_VERSION));
			credentialIssueResponseDto.setStatus(status);
			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			}
		}
		return credentialIssueResponseDto;
	}

	/**
	 * Gets the provider.
	 *
	 * @param credentialType the credential type
	 * @return the provider
	 */
	private CredentialProvider getProvider(String credentialType)
	{
		String provider = credentialFormatterMapperUtil
				.getCredentialFormatterCode(CredentialType.valueOf(credentialType));

		if(provider==null) {
			return defaultProvider;
		}
		else if (provider.equalsIgnoreCase(CredentialFormatter.idAuthProvider.name())) {
			return idAuthProvider;
		} else if (provider.equalsIgnoreCase(CredentialFormatter.pinBasedProvider.name())) {
			return pinBasedProvider;
		} else {
			return defaultProvider;
		}

	}

	/**
	 * Sets the sharable attribute values.
	 *
	 * @param idResponseDto               the id response dto
	 * @param credentialServiceRequestDto the credential service request dto
	 * @param policyDetailResponseDto     the policy detail response dto
	 * @return the map
	 */
	private Map<String, Object> setSharableAttributeValues(IdResponseDto idResponseDto,
			CredentialServiceRequestDto credentialServiceRequestDto, PolicyDetailResponseDto policyDetailResponseDto) {
		// TODO Directly using sharable attributes name from policy AND from input
		// TODO as of now only demographic details are getting shared
		Map<String, Object> attributesMap = new HashMap<>();
		JSONObject identity = new JSONObject((Map) idResponseDto.getResponse().getIdentity());

		List<ShareableAttribute> sharableAttributeList=policyDetailResponseDto.getPolicies().getShareableAttributes();
		Set<String> sharableAttributeKeySet=new HashSet<>();

		if(credentialServiceRequestDto.getSharableAttributes()!=null)
			sharableAttributeKeySet.addAll(credentialServiceRequestDto.getSharableAttributes());

		sharableAttributeList.forEach(dto -> {
			sharableAttributeKeySet.add(dto.getAttributeName());
		});
		for (String key : sharableAttributeKeySet) {
			Object object = identity.get(key);
			if (object instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(identity, key);
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				// TODO is this ok to add example name_eng name_ara as key
				/*for (JsonValue jsonValue : jsonValues) {
					
					if (jsonValue.getLanguage().equals(primaryLang))
						attributesMap.put(key + "_" + primaryLang, jsonValue.getValue());
					if (jsonValue.getLanguage().equals(secondaryLang))
						attributesMap.put(key + "_" + secondaryLang, jsonValue.getValue());

				}*/
				attributesMap.put(key, jsonValues);
			} else if (object instanceof LinkedHashMap) {
				JSONObject json = JsonUtil.getJSONObject(identity, key);
				attributesMap.put(key, (String) json.get(VALUE));
			} else {
				attributesMap.put(key, String.valueOf(object));
			}
		}
		attributesMap.put("id", credentialServiceRequestDto.getId());
		return attributesMap;
	}

	@Override
	public CredentialTypeResponse getCredentialTypes() {
		List<Type> credentialTypes = utilities.getTypes(configServerFileStorageURL, credentialTypefile);
		CredentialTypeResponse CredentialTypeResponse = new CredentialTypeResponse();
		CredentialTypeResponse.setCredentialTypes(credentialTypes);
		return CredentialTypeResponse;
	}



}
