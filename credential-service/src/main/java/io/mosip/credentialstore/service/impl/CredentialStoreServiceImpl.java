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
import org.springframework.stereotype.Component;

import io.mosip.credentialstore.constants.CredentialFormatter;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.CredentialType;
import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.IdResponseDto;
import io.mosip.credentialstore.dto.JsonValue;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;
import io.mosip.credentialstore.dto.Type;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.CredentialServiceException;
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
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credentialstore.service.CredentialStoreService#
	 * createCredentialIssuance(io.mosip.credentialstore.dto.
	 * CredentialServiceRequestDto)
	 */
	public String createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto) {

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
		    throw new CredentialServiceException(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode(),CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage());
		}
		
		} catch (ApiNotAccessibleException | IdRepoException | CredentialFormatterException e) {
			throw new CredentialServiceException(e.getErrorCode(), e.getMessage());
		} catch (IOException e) {
			throw new CredentialServiceException(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorCode(),
					CredentialServiceErrorCodes.IO_EXCEPTION.getErrorMessage());
		} catch (Exception e) {
			throw new CredentialServiceException();
		}

		return status;
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
		return null;
	}

	@Override
	public CredentialTypeResponse getCredentialTypes() {
		List<Type> credentialTypes = utilities.getTypes(configServerFileStorageURL, credentialTypefile);
		CredentialTypeResponse CredentialTypeResponse = new CredentialTypeResponse();
		CredentialTypeResponse.setCredentialTypes(credentialTypes);
		return CredentialTypeResponse;
	}



}
